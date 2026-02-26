package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositRequest;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedCallBack;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.route.processor.DepositProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.bank.BankDepositProcessor;
import com.hybrid9.pg.Lipanasi.services.payments.FailedCallBackService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class FailedInitPaymentCallbackRoute extends RouteBuilder {

    @Autowired
    @Qualifier("depositProcessorVirtualThread")
    private ExecutorService depositProcessorVirtualThread;

    public final DepositProcessor depositProcessor;
    public final BankDepositProcessor bankDepositProcessor;
    public final FailedCallBackService failedCallBackService;
    private final PushUssdService pushUssdService;

    public FailedInitPaymentCallbackRoute(DepositProcessor depositProcessor, FailedCallBackService failedCallBackService, BankDepositProcessor bankDepositProcessor, PushUssdService pushUssdService) {
        this.depositProcessor = depositProcessor;
        this.failedCallBackService = failedCallBackService;
        this.bankDepositProcessor = bankDepositProcessor;
        this.pushUssdService = pushUssdService;
    }

    @Override
    public void configure() throws Exception {
        // Configure global error handling
        configureErrorHandling();

        // Configure routes for deposit processing
        configureConsumerRoute();

        // Configure routes for async callback
        configureAsyncCallbackRoute();

        // Update USSD push status route
        updateUssdStatus();
    }

    private void configureErrorHandling() {
        onException(com.rabbitmq.client.ShutdownSignalException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .handled(true)
                .log(LoggingLevel.ERROR, "RabbitMQ connection error: ${exception.message}");

        onException(Exception.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) // 10 second delay between retries
                .backOffMultiplier(2)  // Exponential backoff
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logRetryAttempted(true)
                .logStackTrace(true)
                .handled(true);
    }


    private void configureConsumerRoute() {
        from(CamelConfiguration.RABBIT_CONSUMER_VENDOR_CALLBACK_URI)
                .routeId("vendor-failed-callback-route")
                .log("Received record from RabbitMQ: ${body}")
                .doTry()
                .process(exchange -> {
                    // Extract values from exchange
                    FailedDepositRequest failedDepositRequest = new ObjectMapper().readValue(exchange.getIn().getBody(String.class),FailedDepositRequest.class);
                    if (failedDepositRequest == null || failedDepositRequest.getReference() == null) {
                        throw new IllegalArgumentException("Invalid failed deposit request");
                    }

                    // Construct idempotency key
                    String idempotencyKey = failedDepositRequest.getTransactionNo() + "-" +
                            failedDepositRequest.getReference() + "-" +
                            failedDepositRequest.getPaymentSessionId() + "-" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    exchange.setProperty("IdempotencyKey", idempotencyKey);

                    // Set callback URL
                    exchange.setProperty("callbackUrl", failedDepositRequest.getCallbackUrl());

                    // Set payment channel
                    exchange.setProperty("paymentChannel", failedDepositRequest.getChannel());

                    exchange.setProperty("pushUssd", this.pushUssdService.findByReference(failedDepositRequest.getReference()));

                    exchange.getIn().setHeader("originalException", failedDepositRequest.getMessage());

                })
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("x-api-key", constant("cd3f2dee-b4c0-47b7-b71c-c35b2c55e8b1"))
                .setHeader("Idempotency-Key", simple("${exchangeProperty.IdempotencyKey}"))
                // Using wireTap for fire-and-forget async callback
                .wireTap("direct:failed-payment-init-async-callback")
                .process(exchange -> {
                    //TODO: Subject to change (pushUssd)
                    exchange.getIn().setHeader("pushUssd", exchange.getProperty("pushUssd", PushUssd.class));
                })
                .choice()
                .when(simple("${exchangeProperty.pushUssd} != null"))
                .to("direct:failed-init-payment-update-ussd-push-status")
                .otherwise()
                .log("Push Ussd not found in header, skipping update")
                .endChoice()
                .end()
                .endDoTry()
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    throw e;
                })
                .end();
    }

    // Fire-and-forget async callback using wireTap
    private void configureAsyncCallbackRoute() {
        from("direct:failed-payment-init-async-callback")
                .routeId("failed-payment-init-async-callback-processor")
                .threads().executorService(depositProcessorVirtualThread)
                .log("Processing async callback: ${body}")
                .doTry()
                .toD("${exchangeProperty.callbackUrl}?bridgeEndpoint=true&httpMethod=POST&connectTimeout=5000&socketTimeout=10000")
                .log("Async callback successful: ${body}")
                .process(exchange -> {
                    // Optionally log successful callback
                    PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                    if (pushUssd != null) {
                        log.info("Callback successful for transaction ID: {}, Reference: {}",
                                pushUssd.getId(), pushUssd.getReference());
                    }
                })
                .doCatch(org.apache.camel.http.base.HttpOperationFailedException.class)
                .log(LoggingLevel.WARN, "Async callback HTTP error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                    if (pushUssd != null) {
                        this.logCallbackFailure(pushUssd, "HTTP Error: " + e.getMessage()).join();
                    }
                })
                .doCatch(java.net.ConnectException.class, java.net.SocketTimeoutException.class)
                .log(LoggingLevel.WARN, "Async callback connection/timeout error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                    if (pushUssd != null) {
                        this.logCallbackFailure(pushUssd, "Connection/Timeout Error: " + e.getMessage()).join();
                    }
                })
                .doCatch(Exception.class)
                .log(LoggingLevel.WARN, "Async callback general error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                    if (pushUssd != null) {
                        this.logCallbackFailure(pushUssd, "General Error: " + e.getMessage()).join();
                    }
                })
                .end();
    }

    private CompletableFuture<Void> logCallbackFailure(PushUssd pushUssd, String callbackError) {
        // Add callback failure to database

        FailedCallBack failedCallBack = FailedCallBack.builder()
                .msisdn(pushUssd.getMsisdn())
                .reference(pushUssd.getReference())
                .originalReference(pushUssd.getReceiptNumber())
                .errorMessage(callbackError)
                .pushUssd(pushUssd)
                .build();

        // Save to database asynchronously
        return CompletableFuture.runAsync(() -> failedCallBackService.createFailedCallBack(failedCallBack), depositProcessorVirtualThread);

    }

    private void updateUssdStatus() {
        // Update USSD push status route
        from("direct:failed-init-payment-update-ussd-push-status")
                .routeId("failed-init-payment-update-ussd-push-status-route")
                .log("Processing update-ussd-push-status: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                    pushUssd.setDetails("USSD Push Initiation Failed, callback sent to merchant");
                    pushUssd.setErrorMessage(exchange.getIn().getHeader("originalException", String.class));
                    this.depositProcessor.updateUssdPushStatusAsync(pushUssd)
                            .thenAcceptAsync(success -> {
                                if (success) {
                                    log.info("[Scoop] Push Ussd Status updated successfully");

                                    exchange.getContext().createProducerTemplate()
                                            .send("direct:airtelmoney-update-deposit-txn-status", exchange);
                                }
                            });

                })
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    throw e;
                })
                .end();
    }
}

