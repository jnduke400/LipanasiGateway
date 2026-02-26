package com.hybrid9.pg.Lipanasi.route.paybill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.PayBillPaymentDto;
import com.hybrid9.pg.Lipanasi.dto.paybill.PaymentResponse;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.resources.ConstructorBuilder;
import com.hybrid9.pg.Lipanasi.route.processor.DepositProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.PayBillProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.payments.TransactionLogServiceImpl;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillDeduplicationService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.nimbusds.jose.shaded.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

//@Component
public class PayBillDepositRouter extends RouteBuilder {

    private final TransactionLogServiceImpl transactionLogServiceImpl;
    private final PushUssdService pushUssdService;
    private final MnoServiceImpl mnoService;
    private final ConstructorBuilder constructorBuilder;
    private final PushUssdRefService pushUssdRefService;
    private final PayBillPaymentService payBillPaymentService;

    public PayBillDepositRouter(PushUssdService pushUssdService, MnoServiceImpl mnoService, ConstructorBuilder constructorBuilder, TransactionLogServiceImpl transactionLogServiceImpl, PushUssdRefService pushUssdRefService, PayBillPaymentService payBillPaymentService ) {
        this.pushUssdService = pushUssdService;
        this.mnoService = mnoService;
        this.constructorBuilder = constructorBuilder;
        this.transactionLogServiceImpl = transactionLogServiceImpl;
        this.pushUssdRefService = pushUssdRefService;
        this.payBillPaymentService = payBillPaymentService;
    }

    @Override
    public void configure() throws Exception {
        Gson gson = new Gson();

        onException(com.rabbitmq.client.ShutdownSignalException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .handled(true)
                .log(LoggingLevel.ERROR, "RabbitMQ connection error: ${exception.message}");
        // Define global error handling strategy
        onException(Exception.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000) // 1 second delay between retries
                .backOffMultiplier(2)  // Exponential backoff
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logRetryAttempted(true)
                .logStackTrace(true)
                .onRedelivery(exchange -> {
                    int retries = exchange.getIn().getHeader("CamelRedeliveryCounter", 0, Integer.class);
                    String msisdn = exchange.getIn().getHeader("msisdn", String.class);
                    String mobileMoney = exchange.getIn().getHeader("mobileMoneyName", String.class);
                    log.warn("Retry attempt {} for message {} and mobile money {}", retries, msisdn, mobileMoney);

                    // Update retry count in database
                    PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                    if (pushUssd != null) {
                        transactionLogServiceImpl.updateRetryCount(pushUssd);
                    }
                })
                .onExceptionOccurred(exchange -> {
                    int retries = exchange.getIn().getHeader("CamelRedeliveryCounter", 0, Integer.class);
                    if (retries >= 3) {
                        PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
                        if (pushUssd != null) {
                            transactionLogServiceImpl.updateToFailed(pushUssd);
                        }
                        log.error("Message processing failed after {} retries. Message moved to failed state.", retries);
                    }
                })
                .handled(true);

        MnoMapping halopesaTanzania = this.mnoService.findMappingByMno("Halopesa-Tanzania");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(halopesaTanzania.getMno());


        //Add Collection Status to a List
        List<CollectionStatus> collectionStatusList = new ArrayList<>();
        collectionStatusList.add(CollectionStatus.COLLECTED);
        collectionStatusList.add(CollectionStatus.FAILED);

        from("quartz://paybill/halopesa?cron=0/50+*+*+*+*+?&stateful=false") // Trigger every 1 second
                .routeId("halopesa-paybill-deposits-producer")
                .transacted() // Transactional
                // Use the repository to fetch data
                .process(exchange -> {
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() ->
                            this.payBillPaymentService.findByCollectionStatusAndOperator(collectionStatusList, mnoList)

                    ).thenApplyAsync(result -> {
                        result.forEach(record -> record.setCollectionStatus(CollectionStatus.PROCESSING));
                        return result;

                    }).thenApplyAsync(this.payBillPaymentService::updateAllCollectionStatus
                    ).thenAcceptAsync(result ->
                            exchange.getIn().setBody(result)
                    );
                    future.join();

                })
                .split(body()) // Split the records to process each individually
                .throttle(10)
                .log("Records fetched: ${body}")
                //.process(exchange -> PushUssdProcessor.process(exchange, this.pushUssdService, gson))
                .process(PayBillProcessor::process)
                // Send records to RabbitMQ
                .to(CamelConfiguration.RABBIT_PRODUCER_HALOPESA_PAY_BILL_VALIDATION_URI)
                .log("Record sent to RabbitMQ: ${body}")
                .end();


        from(CamelConfiguration.RABBIT_CONSUMER_HALOPESA_PAY_BILL_URI)
                .routeId("halopesa-paybill-deposits-consumer")
                .log("Received record from RabbitMQ: ${body}")
                .bean(PayBillDeduplicationService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(exchange -> {
                    process(exchange);

                })
                .choice()
                .when(header("mobileMoneyName").isEqualTo("Halopesa-Tanzania"))
                .log("Mobile Money Name Found: ${header.mobileMoneyName}")
                .to("direct:halopesa-tanzania-paybil-payment-processor")
                .otherwise()
                .log("Mobile Money Name Not Found: ${header.mobileMoneyName}")
                .end();

        // mobile-money-tanzania-payment-processor
        from("direct:halopesa-tanzania-paybil-payment-processor")
                .throttle(halopesaTanzania.getTps())
                .routeId("halopesa-tanzania-paybil-payment-processor")
                .log("Processing Halopesa-Tanzania PayBill Payment: ${body}")
                .doTry()
                .process(exchange -> {
                    DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    depositProcessor.initPayBillDeposit(exchange.getIn().getHeader("payBillPayment", PayBillPayment.class));
                    /*PayBillPayment payBillPayment = exchange.getIn().getHeader("payBillPayment", PayBillPayment.class);*/

                    /*ApiPayload apiPayload = ApiPayload.builder()
                            .msisdn(exchange.getIn().getHeader("msisdn", String.class))
                            .responseMessage(exchange.getIn().getHeader("message", String.class))
                            .txnStatus(pushUssd.getCollectionStatus().name().equalsIgnoreCase("MARKED_FOR_DEPOSIT") ? "SUCCESS" : "FAILED")
                            .reference(pushUssd.getReference())
                            .amount(pushUssd.getAmount())
                            .build();*/

                    //TODO: Subject to change

                    /*DepositRequest depositRequest = DepositRequest.builder()
                            .transactionNo(payBillPayment.getReceiptNumber())
                            .reference(payBillPayment.getPaymentReference())
                            //.message(exchange.getIn().getHeader("message", String.class))
                            //.status(pushUssd.getStatus().equalsIgnoreCase("0") ? "SUCCESS" : "FAILED")
                            .build();

                    ObjectMapper mapper = new ObjectMapper();
                    String apiJsonRq = mapper.writeValueAsString(depositRequest);
                    System.out.println("api payload is " + apiJsonRq);
                    exchange.getIn().setBody(apiJsonRq);*/


                })
                .to("direct:initiatePayBillDeposit")
                .process(exchange -> {
                    System.out.println("Deposit Response Body: ${body}");
                    // update both balance and ledger for halopesa
                    String responseMessage = exchange.getIn().getBody(String.class);

                    ObjectMapper mapper = new ObjectMapper();
                    PaymentResponse message = mapper.readValue(responseMessage, PaymentResponse.class);

                    // Process the response body as needed
                    //TODO: Subject to change
                    DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    depositProcessor.initiatePayBillDeposit(message, exchange.getIn().getHeader("payBillPayment", PayBillPayment.class));

                })
                //TODO: Subject to change
                //.to("direct:halopesa-update-paybill-account-balance")
                .log("Deposit Confirmation Body: ${body}")
                .to("direct:confirmPayBillDeposit")
                .to("direct:halopesa-update-paybill-account-balance")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    // The global error handler will handle retries
                    throw e;
                })
                .end();

        from("direct:halopesa-update-paybill-account-balance")
                .routeId("halopesa-update-paybill-account-balance")
                .log("Processing halopesa-update-paybill-account-balance: ${body}")
                .doTry()
                .process(exchange -> {
                    DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    depositProcessor.updatePayBillDepositBalance(exchange.getIn().getHeader("payBillPayment", PayBillPayment.class));
                })
                .to("direct:halopesa-update-paybill-status")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    // The global error handler will handle retries
                    throw e;
                })
                .end();

        from("direct:halopesa-update-paybill-status")
                .routeId("halopesa-update-paybill-status")
                .log("Processing halopesa-update-paybill-status: ${body}")
                .doTry()
                .process(exchange -> {
                    DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    depositProcessor.updatePayBillStatus(exchange.getIn().getHeader("payBillPayment", PayBillPayment.class));
                })
                //.to("direct:halopesa-update-deposit-txn-status")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    // The global error handler will handle retries
                    throw e;
                })
                .end();

        /*from("direct:halopesa-update-deposit-txn-status")
                .routeId("halopesa-update-deposit-txn-status")
                .log("Processing update-deposit-txn-status: ${body}")
                .doTry()
                .process(exchange -> {
                    DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    depositProcessor.updateDepositTransactionStatus(exchange.getIn().getHeader("pushUssd", PushUssd.class));
                })
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    // The global error handler will handle retries
                    throw e;
                })
                .end();*/
    }

    private void process(Exchange exchange) {
        ObjectMapper mapper = new ObjectMapper();
        PayBillPaymentDto payBillPaymentDto = null;
        try {
            payBillPaymentDto = mapper.readValue(exchange.getIn().getBody(String.class), PayBillPaymentDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        AtomicLong accountId = new AtomicLong(0L);
        AtomicReference<PayBillPayment> payBillPayment = new AtomicReference<>();
        this.payBillPaymentService.findById(payBillPaymentDto.getId()).ifPresent(payBillPayment1 -> {
            accountId.set(Long.parseLong(payBillPayment1.getAccountId()));
            payBillPayment.set(payBillPayment1);
        });
        String mobileMoneyName = this.mnoService.searchMno(payBillPaymentDto.getMsisdn());
        exchange.getIn().setHeader("mobileMoneyName", mobileMoneyName);
        /*exchange.getIn().setHeader("message", payBillPaymentDto.getMessage());*/
        exchange.getIn().setHeader("msisdn", payBillPaymentDto.getMsisdn());
        exchange.getIn().setHeader("payBillPaymentId", payBillPaymentDto.getId());
        exchange.getIn().setHeader("mainAccountId", accountId.get());
        exchange.getIn().setHeader("payBillPayment", payBillPayment.get());

        exchange.setProperty("mobileMoneyName", mobileMoneyName);
    }
}

