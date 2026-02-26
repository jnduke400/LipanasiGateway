package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.dto.mpesa.LoginResponse;
import com.hybrid9.pg.Lipanasi.dto.mpesa.TransactionResponse;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.AirtelMoneyConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MPesaConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.route.handler.LoginResponseHandler;
import com.hybrid9.pg.Lipanasi.route.handler.SessionIdExtractor;
import com.hybrid9.pg.Lipanasi.route.handler.TransactionResponseHandler;
import com.hybrid9.pg.Lipanasi.route.handler.exceptions.ScoopExceptionHandlers;
import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.MpesaCircuitBreakerProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.MpesaCircuitBreakerProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.CircuitBreakerService;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.InitDepositDeduplicationService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.apache.http.client.HttpClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

//@Component
public class IPGUssdPushRoute extends RouteBuilder {


    private static final String EMAIL_ADDRESS = "ndukep@gmail.com";
    private static final int MAX_RETRIES = 3;

    @Autowired
    @Qualifier("mpesaThreadPool")
    private ThreadPoolTaskExecutor mpesaThreadPool;

    @Autowired
    @Qualifier("mpesaHttpClient")
    private HttpClient mpesaHttpClient;

    @Autowired
    @Qualifier("mpesaCircuitBreaker")
    private CircuitBreaker mpesaCircuitBreaker;

    @Autowired
    @Qualifier("vodacomInitRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    private final CircuitBreakerService circuitBreakerService;
    private final ServiceNameComponent serviceNameComponent;
    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;
    private final SessionManagementService sessionManagementService;
    private final ScoopExceptionHandlers scoopExceptionHandlers;


    public IPGUssdPushRoute(CashInLogService cashInLogService, MnoServiceImpl mnoService,
                            DepositService depositService, MainAccountService mainAccountService,
                            VendorService vendorService,
                            PushUssdService pushUssdService, ServiceNameComponent serviceNameComponent,
                            CircuitBreakerService circuitBreakerService,
                            SessionManagementService sessionManagementService,
                            ScoopExceptionHandlers scoopExceptionHandlers) {
        this.cashInLogService = cashInLogService;
        this.mnoService = mnoService;
        this.depositService = depositService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.pushUssdService = pushUssdService;
        this.circuitBreakerService = circuitBreakerService;
        this.serviceNameComponent = serviceNameComponent;
        this.sessionManagementService = sessionManagementService;
        this.scoopExceptionHandlers = scoopExceptionHandlers;
    }

    @Override
    public void configure() throws Exception {
        //Register Http Client
        getContext().getRegistry().bind("mpesaHttpClient", mpesaHttpClient);

        serviceNameComponent.setServiceName(ServiceName.MPESA_PAYMENT);

        MpesaCircuitBreakerProcessor mpesaCircuitProcessor = new MpesaCircuitBreakerProcessor(
                mpesaCircuitBreaker,
                cashInLogService,
                depositService,
                serviceNameComponent
        );
        // Register Processor
        getContext().getRegistry().bind("mpesaCircuitProcessor", mpesaCircuitProcessor);

        // Error Handler with conditional retry policy
        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(MAX_RETRIES)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retryWhile(this::shouldRetryCheck)
                .onRedelivery(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
                    String errorPrefix = "Mpesa payment failed: ";

                    //  Get correct retry count from Camel's redelivery counter
                    int retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);

                    log.info("Retry Count: {} (Max: {})", retryCount, MAX_RETRIES);

                    // Only record circuit breaker failure for service-related issues
                    if (!(exception instanceof MpesaCircuitBreakerProcessor.CircuitBreakerOpenException)
                            && this.scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
                        circuitBreakerService.recordFailure("mpesaPaymentService", exception);
                    }

                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        if (cashInLog != null) {
                            //  Always update retry count and status appropriately
                            cashInLog.setRetryCount(retryCount);

                            if (retryCount >= MAX_RETRIES) {
                                // Final failure - no more retries
                                cashInLog.setStatus(status);
                                cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
                                //cashInLog.setPaymentReference(exchange.getIn().getHeader("reference", String.class));

                                // Update deposit transaction on final failure
                                updateDepositOnFinalFailure(exchange, status, exception);

                                // Update session
                                String sessionId = exchange.getProperty("sessionId", String.class);
                                if (sessionId != null) {
                                    updateSession(sessionId, "FAILED", "Payment processing failed");
                                    exchange.setProperty("failedMessage", "Payment initiation failed");
                                    // Send callback to vendor
                                    exchange.getContext().createProducerTemplate().send("direct:mpesa-tanzania-init-vendor-callback", exchange);
                                }
                            } else {
                                // Still retrying
                                cashInLog.setStatus(RequestStatus.MARKED_FOR_RETRY);
                                cashInLog.setErrorMessage(errorPrefix + exception.getMessage() + " (Retry " + retryCount + "/" + MAX_RETRIES + ")");
                                //cashInLog.setPaymentReference(exchange.getIn().getHeader("reference", String.class));
                            }

                            cashInLogService.recordLog(cashInLog);
                        }
                    }
                })
                .log("Error processing transaction after ${header.CamelRedeliveryCounter} attempts: ${exception.message}")
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    TransactionResponse errorResponse = new TransactionResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setResponseCode("-1");
                    errorResponse.setDescription("Processing Error");
                    errorResponse.setDetail(exception.getMessage());
                    exchange.getMessage().setBody(errorResponse);
                });

        MnoMapping mpesaTanzania = this.mnoService.findMappingByMno("Mpesa-Tanzania");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(mpesaTanzania.getMno());


        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);

        from("quartz://depositInitiation/mpesa?cron=0/1+*+*+*+*+?&stateful=false") // Trigger every 1 second
                .routeId("init-ussd-mpesa-deposits-producer")
                .threads().executorService(mpesaThreadPool.getThreadPoolExecutor())
                // Use the repository to fetch data
                .process(exchange -> {
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() ->
                            this.depositService.findByRequestStatusAndOperator(requestStatuses, mnoList)

                    ).thenApplyAsync(result -> {
                        result.forEach(record -> record.setRequestStatus(RequestStatus.INITIATED));
                        return result;

                    }).thenApplyAsync(this.depositService::updateAllRequestStatus
                    ).thenAcceptAsync(result ->
                            exchange.getIn().setBody(result)
                    );
                    future.join();

                })
                .transacted() // Transactional
                .split(body()) // Split the records to process each individually
                .log("Records fetched: ${body}")
                //.process(exchange -> InitDepositProcessor.process(exchange))
                .process(exchange -> {
                    Deposit deposit = exchange.getIn().getBody(Deposit.class);
                    DepositDto depositDto = DepositDto.builder()
                            .id(deposit.getId())
                            .msisdn(deposit.getMsisdn())
                            .amount(deposit.getAmount())
                            .channel(deposit.getChannel())
                            .paymentReference(deposit.getPaymentReference())
                            .originalReference(deposit.getOriginalReference())
                            .transactionId(deposit.getTransactionId())
                            .operator(deposit.getOperator())
                            .requestStatus(deposit.getRequestStatus())
                            .sessionId(deposit.getSessionId())
                            .vendorDto(VendorDto.builder()
                                    .vendorName(deposit.getVendorDetails().getVendorName())
                                    .vendorCode(deposit.getVendorDetails().getVendorCode())
                                    .billNumber(deposit.getVendorDetails().getBillNumber())
                                    .hasCommission(deposit.getVendorDetails().getHasCommission())
                                    .hasVat(deposit.getVendorDetails().getHasVat())
                                    .charges(deposit.getVendorDetails().getCharges())
                                    .build())
                            .build();

                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(depositDto);
                    exchange.getIn().setBody(json);
                })
                .process(exchange -> {
                    Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
                    if (channel != null) {
                        channel.confirmSelect(); // Enable publisher confirms
                        exchange.setProperty("rabbitmqChannel", channel);
                        exchange.setProperty("originalMessage", exchange.getIn().getBody());
                    }
                })
                // Send records to RabbitMQ
                .toD(CamelConfiguration.RABBIT_PRODUCER_MPESA_INIT_URI)
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();
                    DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);
                    Channel channel = (Channel) exchange.getProperty("rabbitmqChannel");
                    if (channel != null) {
                        // asynchronous confirm listener
                        channel.addConfirmListener(new ConfirmListener() {
                            @Override
                            public void handleAck(long deliveryTag, boolean multiple) {
                                log.debug("Message confirmed with deliveryTag: {}", deliveryTag);
                            }

                            @Override
                            public void handleNack(long deliveryTag, boolean multiple) {
                                Object originalMessage = exchange.getProperty("originalMessage");
                                log.error("Message NOT confirmed with deliveryTag: {}", deliveryTag);

                                // Retry logic or dead letter handling
                                handleNackedMessage(originalMessage, deliveryTag,depositDto.getSessionId());
                            }
                        });
                    }
                })
                .log("Record sent to RabbitMQ: ${body}")
                .end();

        // Login Route
        from(CamelConfiguration.RABBIT_CONSUMER_MPESA_INIT_URI)
                .routeId("init-ussd-mpesa-deposits-consumer")
                .log("Received record from RabbitMQ: ${body}")
                .bean(InitDepositDeduplicationService.class, "checkAndMarkInitDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(exchange -> {
                    //set header
                    ObjectMapper mapper = new ObjectMapper();
                    DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);
                    exchange.getIn().setHeader("msisdn", depositDto.getMsisdn());
                    exchange.getIn().setHeader("amount", depositDto.getAmount());
                    exchange.getIn().setHeader("reference", depositDto.getPaymentReference());
                    exchange.getIn().setHeader("operator", depositDto.getOperator());
                    exchange.getIn().setHeader("transactionId", depositDto.getTransactionId());
                    exchange.setProperty("sessionId", depositDto.getSessionId());
                    exchange.getIn().setHeader("originalReference", depositDto.getOriginalReference());
                    exchange.setProperty("networkConfig", (MPesaConfig) this.sessionManagementService.getSession(depositDto.getSessionId()).orElseThrow(() ->
                            new RuntimeException("Session not found for ID: " + depositDto.getSessionId())).getNetworkConfig());
                    // Create initial log entry
                    CashInLog cashInLog = CashInLog.builder()
                            .cashInRequest(exchange.getMessage().getBody(String.class))
                            .retryCount(0)
                            .status(RequestStatus.INITIATED)
                            .paymentReference(depositDto.getPaymentReference())
                            .build();
                    cashInLogService.recordLog(cashInLog);
                    exchange.setProperty("cashInLogId", cashInLog.getId());
                })
                .process("mpesaCircuitProcessor")
                .log("Initiating login request")
                .setHeader("Content-Type", constant("application/xml"))
                .process(exchange -> {
                    // Get the network config from exchange property
                    MPesaConfig networkConfig = exchange.getProperty("networkConfig", MPesaConfig.class);

                    if (networkConfig == null) {
                        throw new RuntimeException("Network configuration not found in exchange properties");
                    }

                    // Validate that tokenApiUrl is not null or empty
                    if (networkConfig.getTokenApiUrl() == null || networkConfig.getTokenApiUrl().trim().isEmpty()) {
                        throw new RuntimeException("Token API URL is not configured in network config");
                    }

                    String loginXml = String.format("""
                            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                              xmlns:soap="http://www.4cgroup.co.za/soapauth"
                                              xmlns:gen="http://www.4cgroup.co.za/genericsoap">
                                <soapenv:Header>
                                    <soap:Token>?</soap:Token>
                                    <soap:EventID>%s</soap:EventID>
                                </soapenv:Header>
                                <soapenv:Body>
                                    <gen:getGenericResult>
                                        <Request>
                                            <dataItem>
                                                <name>Username</name>
                                                <type>String</type>
                                                <value>%s</value>
                                            </dataItem>
                                            <dataItem>
                                                <name>Password</name>
                                                <type>String</type>
                                                <value>%s</value>
                                            </dataItem>
                                        </Request>
                                    </gen:getGenericResult>
                                </soapenv:Body>
                            </soapenv:Envelope>
                            """, networkConfig.getTokenEventId(), networkConfig.getUsername(), networkConfig.getPassword());

                    exchange.getMessage().setBody(loginXml);

                    // Store the URL in exchange property for use in the HTTP call
                    exchange.setProperty("tokenApiUrl", networkConfig.getTokenApiUrl());

                    log.info("Login XML: " + loginXml);
                    log.info("Token API URL: " + networkConfig.getTokenApiUrl());
                })
                .setHeader("User-Agent", constant("Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)"))
                .setHeader("CamelHttpClient", constant(mpesaHttpClient))
                //TODO: Remove this line once the token API is fixed
                /*.toD("${exchangeProperty.tokenApiUrl}?bridgeEndpoint=true&httpMethod=POST" +
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false" +
                        "&sslContextParameters=#noopSslContext")*/
                .toD("${exchangeProperty.tokenApiUrl}?bridgeEndpoint=true&httpMethod=POST" +
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false")
                .log("Login response received: ${body}")
                .process(LoginResponseHandler::process)
                .process(exchange -> {
                    // Record successful call to circuit breaker
                    circuitBreakerService.recordSuccess("mpesaPaymentService");
                })
                .choice()
                .when(header("LoginStatus").isEqualTo("SUCCESS"))
                .log("Login successful, proceeding with transaction")
                .process(exchange -> {
                    LoginResponse loginResponse = exchange.getIn().getBody(LoginResponse.class);
                    SessionIdExtractor.process(exchange, loginResponse);
                })
                .toD("direct:initiateTransaction")
                .otherwise()
                //.toD("direct:handleLoginFailure")
                .log("Login failed, stopping process")
                .end()
                .process(exchange -> {
                    // Acknowledge the message in all cases
                    Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
                    Long deliveryTag = exchange.getIn().getHeader(SpringRabbitMQConstants.DELIVERY_TAG, Long.class);
                    if (channel != null && deliveryTag != null) {
                        try {
                            channel.basicAck(deliveryTag, false);
                        } catch (IOException e) {
                            log.error("Failed to acknowledge RabbitMQ message", e);
                            // Consider adding retry logic or dead letter handling here
                        }
                    }
                });


        // Handle login failure route
        from("direct:handleLoginFailure")
                .routeId("loginFailureRoute")
                .log("Processing login failure")
                .process(exchange -> {
                    LoginResponse response = exchange.getMessage().getBody(LoginResponse.class);
                    // You can add additional failure handling logic here
                    // For example, notify monitoring systems, update metrics, etc.
                });

        // Transaction Initiation Route
        from("direct:initiateTransaction")
                .routeId("transactionRoute")
                .log("Initiating Mpesa USSD Push transaction")
                .setHeader("Content-Type", constant("text/xml"))
                .process(exchange -> {
                    // Get the network config from exchange property
                    MPesaConfig networkConfig = exchange.getProperty("networkConfig", MPesaConfig.class);

                    if (networkConfig == null) {
                        throw new RuntimeException("Network configuration not found in exchange properties");
                    }

                    String sessionId = exchange.getMessage().getHeader("SessionId", String.class);
                    System.out.println("Session >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + sessionId);
                    String transactionXml =
                            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                                    "xmlns:gen=\"http://www.4cgroup.co.za/genericsoap\" " +
                                    "xmlns:soap=\"http://www.4cgroup.co.za/soapauth\">" +
                                    "<soapenv:Header>" +
                                    "<soap:Token>" + sessionId + "</soap:Token>" +
                                    "<soap:EventID>" + networkConfig.getRequestEventId() + "</soap:EventID>" +
                                    "</soapenv:Header>" +
                                    "<soapenv:Body>" +
                                    "<gen:getGenericResult>" +
                                    "<Request>" +
                                    createDataItem("CustomerMSISDN", exchange.getMessage().getHeader("msisdn", String.class)) +
                                    createDataItem("BusinessName", networkConfig.getBusinessName()) +
                                    createDataItem("BusinessNumber", networkConfig.getBusinessNumber()) +
                                    createDataItem("Currency", "TZS") +
                                    createDataItem("Date", getCurrentFormattedDate()) +
                                    createDataItem("Amount", exchange.getMessage().getHeader("amount", String.class)) +
                                    createDataItem("ThirdPartyReference", exchange.getMessage().getHeader("reference", String.class)) +
                                    createDataItem("Command", "customerPayBill") +
                                    createDataItem("CallBackChannel", "1") +
                                    createDataItem("CallbackDestination", networkConfig.getCallbackUrl()) +
                                    createDataItem("Username", networkConfig.getUsername()) +
                                    "</Request>" +
                                    "</gen:getGenericResult>" +
                                    "</soapenv:Body>" +
                                    "</soapenv:Envelope>";
                    System.out.println("Transaction >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + transactionXml);
                    exchange.getMessage().setBody(transactionXml);
                })
                .setHeader("User-Agent", constant("Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)"))
                .log("Mpesa USSD Push Request Body: ${body}")
                .setHeader("CamelHttpClient", constant(mpesaHttpClient))
                //TODO: Remove this line once the token API is fixed
                /*.toD("${exchangeProperty.networkConfig.apiUrl}?bridgeEndpoint=true&httpMethod=POST" +
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false" +
                        "&sslContextParameters=#noopSslContext")*/
                .toD("${exchangeProperty.networkConfig.apiUrl}?bridgeEndpoint=true&httpMethod=POST" +
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false")
                .log("Mpesa USSD Push Response: ${body}")
                .process(exchange -> new TransactionResponseHandler().process(exchange))
                .toD("direct:mpesaTransactionResponse")
                .end();

        /*from("direct:handleTransactionFailure")
                .log("Processing transaction failure: ${body}");*/

        from("direct:mpesaTransactionResponse")
                .routeId("mpesaTransactionResponseRoute")
                .process(exchange -> {
                    this.depositService.findByReference(exchange.getMessage().getHeader("reference", String.class)).ifPresent(deposit -> {
                        PushUssd pushUssd = PushUssd.builder()
                                .status(exchange.getMessage().getBody(TransactionResponse.class).getResponseCode())
                                .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                .message(exchange.getMessage().getBody(TransactionResponse.class).getResponseCode().equals("0") ? "Ussd Push Initiated Successfully" : "Ussd Push Initiated Failed")
                                .reference(exchange.getMessage().getBody(TransactionResponse.class).getThirdPartyReference())
                                .currency(deposit.getCurrency())
                                .operator(this.mnoService.searchMno(deposit.getMsisdn()))
                                .transactionNumber(exchange.getMessage().getBody(TransactionResponse.class).getInsightReference())
                                .accountId(String.valueOf(mainAccountService.findByVendorDetails(deposit.getVendorDetails()).getId()))
                                .vendorDetails(deposit.getVendorDetails())
                                .sessionId(deposit.getSessionId())
                                .msisdn(deposit.getMsisdn())
                                .build();
                        this.pushUssdService.update(pushUssd);

                        // Update session
                        String sessionId = exchange.getProperty("sessionId", String.class);
                        if (sessionId != null && !exchange.getMessage().getBody(TransactionResponse.class).getResponseCode().equals("0")) {
                            updateSession(sessionId, "FAILED", "Payment processing failed");
                            exchange.setProperty("failedMessage", "Payment initiation failed");
                            // Send callback to vendor
                            exchange.getContext().createProducerTemplate().send("direct:mpesa-tanzania-init-vendor-callback", exchange);
                        }
                    });
                })
                .log("Waiting for callback for transaction: ${header.transactionId}")
                .end();

        // Vendor Callback route
        from("direct:mpesa-tanzania-init-vendor-callback")
                .routeId("mpesa-tanzania-init-vendor-callback-route")
                .log("Processing vendor callback: ${body}")
                .doTry()
                .process(exchange -> {
                    // Restore original body for later restoration
                    exchange.setProperty("originalJsonBody", exchange.getIn().getBody());
                })
                .process(exchange -> {
                    log.info("Processing Vendor Callback: " + exchange.getIn().getBody(String.class));
                    // Prepare vendor callback
                    this.processVendorCallback(exchange, exchange.getProperty("sessionId", String.class));
                })
                .toD(CamelConfiguration.RABBIT_PRODUCER_VENDOR_CALLBACK_URI)
                .log("Recorded vendor callback: ${body}")
                .setBody(simple("${exchangeProperty.originalJsonBody}"))
                .process(exchange -> {
                    // Acknowledge the original message
                    Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
                    Long deliveryTag = exchange.getIn().getHeader(SpringRabbitMQConstants.DELIVERY_TAG, Long.class);
                    if (channel != null && deliveryTag != null) {
                        try {
                            channel.basicAck(deliveryTag, false);
                        } catch (IOException e) {
                            log.error("Failed to acknowledge RabbitMQ message in vendor callback", e);
                            throw new RuntimeException("Failed to acknowledge message", e);
                        }
                    }
                })
                .endDoTry() // End of try block
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing transaction after ${header.CamelRedeliveryCounter} attempts: ${exception.message}")
                .end()
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .end();

    }
    // Immediately retry the message after a nack (for transient errors)
    private void handleNackedMessage(Object message, long deliveryTag, String sessionId) {
        int maxRetries = 3;
        int retryDelayMs = 1000; // 1 second delay

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Re-publish the message
                rabbitTemplate.convertAndSend(
                        CamelConfiguration.RABBIT_PRODUCER_MPESA_INIT_URI,
                        message
                );
                log.info("Retry attempt {} successful for deliveryTag: {}", attempt, deliveryTag);
                return;
            } catch (Exception e) {
                log.warn("Retry attempt {} failed for deliveryTag: {}", attempt, deliveryTag, e);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // If all retries fail, send to dead letter queue
        //sendToDeadLetterQueue(message, deliveryTag);

        // for ussd push initiation route, we need to update the session status to failed
        updateSession(sessionId, "FAILED", "Payment processing failed");
    }
    private void processVendorCallback(Exchange exchange, String sessionId) {
        AtomicReference<Deposit> depositReference = new AtomicReference<>();
        this.depositService.findBySessionId(sessionId).ifPresent(depositReference::set);
        // Build deposit request object
        FailedDepositRequest depositRequest = FailedDepositRequest.builder()
                .transactionNo(depositReference.get().getTransactionId())
                .reference(depositReference.get().getOriginalReference())
                .message(exchange.getProperty("failedMessage", String.class))
                .status("FAILED")
                .paymentSessionId(exchange.getProperty("sessionId", String.class))
                .callbackUrl(depositReference.get().getVendorDetails().getCallbackUrl())
                .build();

        // Convert deposit request to JSON
        ObjectMapper mapper = new ObjectMapper();
        String apiJsonRq = null;
        try {
            apiJsonRq = mapper.writeValueAsString(depositRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        exchange.getIn().setBody(apiJsonRq);
    }

    private String createDataItem(String name, String value) {
        return "<dataItem>" +
                "<name>" + name + "</name>" +
                "<type>String</type>" +
                "<value>" + value + "</value>" +
                "</dataItem>";
    }

    private String getCurrentFormattedDate() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String generateUniqueReference() {
        return UUID.randomUUID().toString();
    }

    private void logRetryAttempt(Exchange exchange, String operation, int retryCount) {
        log.info("{} retry attempt {} of {}", operation, retryCount, MAX_RETRIES);

        Long cashInLogId = exchange.getProperty("cashInLogId", Long.class);
        if (cashInLogId != null) {
            CashInLog cashInLog = cashInLogService.findById(cashInLogId).orElse(null);
            if (cashInLog != null) {
                cashInLog.setRetryCount(retryCount);
                cashInLog.setStatus(retryCount >= MAX_RETRIES ?
                        RequestStatus.FAILED : RequestStatus.MARKED_FOR_RETRY);
                //cashInLog.setPaymentReference(exchange.getIn().getHeader("reference", String.class));
                cashInLogService.recordLog(cashInLog);
            }
        }
    }

    private void updateCashInLog(Exchange exchange, int retryCount) {
        Long cashInLogId = exchange.getProperty("cashInLogId", Long.class);
        if (cashInLogId != null) {
            CashInLog cashInLog = cashInLogService.findById(cashInLogId).orElse(null);
            if (cashInLog != null) {
                if (retryCount >= MAX_RETRIES) {
                    cashInLog.setStatus(RequestStatus.FAILED);
                    //cashInLog.setPaymentReference(exchange.getIn().getHeader("reference", String.class));
                } else {
                    cashInLog.setStatus(RequestStatus.MARKED_FOR_RETRY);
                    //cashInLog.setPaymentReference(exchange.getIn().getHeader("reference", String.class));
                }
                cashInLog.setRetryCount(retryCount);
                cashInLogService.recordLog(cashInLog);
            }
        }
    }

    private int getRetryCount(Exchange exchange) {
        Integer retryCount = exchange.getProperty(Exchange.REDELIVERY_COUNTER, Integer.class);
        return retryCount != null ? retryCount : 0;
    }

    /*private boolean shouldRetryCheck(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean shouldRetry = this.scoopExceptionHandlers.isRetryableException(exception);

        if (!shouldRetry) {
            log.info("Exception {} is non-retryable, skipping retry",
                    exception.getClass().getSimpleName());
            updateStatusForNonRetryableException(exchange, exception);
        }

        return shouldRetry;
    }*/

    /*private boolean shouldRetryCheck(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean isRetryable = this.scoopExceptionHandlers.isRetryableException(exception);

        //  Use CamelRedeliveryCounter instead of Exchange.REDELIVERY_COUNTER
        Integer retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
        if (retryCount == null) retryCount = 0;

        // Only retry if it's a retryable exception AND we haven't exceeded max retries
        boolean shouldRetry = isRetryable && retryCount <= MAX_RETRIES;

        if(retryCount <= MAX_RETRIES && retryCount > 1) {
            log.info("Retry check - Exception: {}, Retryable: {}, Retry Count: {}/{}, Should Retry: {}",
                    exception.getClass().getSimpleName(), isRetryable, retryCount, MAX_RETRIES, shouldRetry);
        }

        if (!shouldRetry && retryCount == 1) {
            log.info("Stopping retries - Exception {} is non-retryable or max retries reached",
                    exception.getClass().getSimpleName());
            updateStatusForNonRetryableException(exchange, exception);
        }

        return shouldRetry;
    }*/

    private boolean shouldRetryCheck(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean isRetryable = this.scoopExceptionHandlers.isRetryableException(exception);

        // Use CamelRedeliveryCounter instead of Exchange.REDELIVERY_COUNTER
        Integer retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
        if (retryCount == null) retryCount = 0;

        // Check if exception is not retryable - handle immediately
        if (!isRetryable) {
            log.info("Exception {} is non-retryable - stopping retries",
                    exception.getClass().getSimpleName());
            updateStatusForNonRetryableException(exchange, exception);
            return false;
        }

        // Only retry if we haven't exceeded max retries (exception is already confirmed retryable)
        boolean shouldRetry = retryCount <= MAX_RETRIES;

        // Log only for retryable exceptions
        if (isRetryable && retryCount < MAX_RETRIES) {
            log.info("Retry check - Exception: {}, Retryable: {}, Retry Count: {}/{}, Should Retry: {}",
                    exception.getClass().getSimpleName(), isRetryable, retryCount, MAX_RETRIES, shouldRetry);
        }

        if (!shouldRetry) {
            log.info("Max retries reached for retryable exception {}",
                    exception.getClass().getSimpleName());
            // Don't call updateStatusForNonRetryableException here since this will be handled in onRedelivery
        }

        return shouldRetry;
    }

    // Helper method to update status for non-retryable exceptions immediately
    private void updateStatusForNonRetryableException(Exchange exchange, Exception exception) {
        RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
        String errorPrefix = "Mpesa payment failed (non-retryable): ";

        // Update cash_in_logs
        String cashInLogId = exchange.getProperty("cashInLogId", String.class);
        if (cashInLogId != null) {
            CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
            if (cashInLog != null) {
                cashInLog.setStatus(status);
                cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
                //cashInLog.setPaymentReference(exchange.getIn().getHeader("reference", String.class));
                cashInLogService.recordLog(cashInLog);
            }
        }

        // Update deposit transaction
        String transactionId = exchange.getProperty("transactionId", String.class);
        if (transactionId != null) {
            depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                deposit.setRequestStatus(status);
                deposit.setErrorMessage(getErrorMessageForStatus(status, null));
                depositService.recordDeposit(deposit);
            });
        }
    }

    // Helper method to get user-friendly error messages
    private String getErrorMessageForStatus(RequestStatus status, Integer responseCode) {
        return switch (status) {
            case NETWORK_CONNECTION_ISSUE -> "Could not connect to Mpesa servers";
            case TIMEOUT -> "Mpesa service did not respond in time";
            case SERVICE_UNAVAILABLE -> responseCode != null && responseCode == 429
                    ? "Too many requests - please try again later"
                    : "Mpesa service is currently unavailable";
            case INVALID_CREDENTIALS -> "Authentication failed with Mpesa service";
            case INVALID_REQUEST -> "Invalid request format or parameters";
            case BUSINESS_RULE_VIOLATION -> "Transaction violates business rules";
            case RESOURCE_NOT_FOUND -> "Requested resource not found";
            default -> "Payment processing failed" + (responseCode != null ? " (HTTP " + responseCode + ")" : "");
        };
    }

    private void updateDepositOnFinalFailure(Exchange exchange, RequestStatus status, Exception exception) {
        String transactionId = exchange.getProperty("transactionId", String.class);
        if (transactionId != null) {
            log.info("Updating deposit status for transaction: {}", transactionId);
            depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                deposit.setRequestStatus(status);

                String errorMessage = switch (status) {
                    case NETWORK_CONNECTION_ISSUE -> "Could not connect to Mpesa servers";
                    case TIMEOUT -> "Mpesa service did not respond in time";
                    case SERVICE_UNAVAILABLE -> "Mpesa service is currently unavailable";
                    case INVALID_CREDENTIALS -> "Authentication failed with Mpesa service";
                    case INVALID_REQUEST -> "Invalid request format or parameters";
                    case BUSINESS_RULE_VIOLATION -> "Transaction violates business rules";
                    default -> "Payment processing failed: " + exception.getMessage();
                };

                deposit.setErrorMessage(errorMessage);
                depositService.recordDeposit(deposit);
            });
        }
    }

    private void updateSession(String sessionId, String status, String errorMessage) {
        UserSession s = sessionManagementService.getSession(sessionId).orElseThrow(() -> new RuntimeException("Session not found for ID: " + sessionId));
        s.setTransactionStatus(status.equalsIgnoreCase("FAILED") ? UserSession.TransactionStatus.FAILED : UserSession.TransactionStatus.PENDING);

        sessionManagementService.updateTransactionStatusAndError(
                sessionId,
                UserSession.TransactionStatus.FAILED,
                errorMessage
        );
    }
}


