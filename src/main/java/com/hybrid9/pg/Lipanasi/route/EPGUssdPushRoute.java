package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.component.halopesa.PaymentGatewayResponse;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.HaloPesaConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.route.handler.exceptions.ScoopExceptionHandlers;

import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.HalopesaCircuitBreakerProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.CircuitBreakerService;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.InitDepositDeduplicationService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

//@Component
public class EPGUssdPushRoute extends RouteBuilder {
    @Autowired
    @Qualifier("halopesaThreadPool")
    private ThreadPoolTaskExecutor halopesaThreadPool;

    @Autowired
    @Qualifier("halopesaHttpClient")
    private HttpClient halopesaHttpClient;

    @Autowired
    @Qualifier("halopesaCircuitBreaker")
    private CircuitBreaker halopesaCircuitBreaker;

    @Autowired
    @Qualifier("halopesaInitsRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    private final CircuitBreakerService circuitBreakerService;
    private final ServiceNameComponent serviceNameComponent;
    private static final String EMAIL_ADDRESS = "ndukep@gmail.com";
    private static final int MAX_RETRIES = 3;
    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;
    private final SessionManagementService sessionManagementService;
    private final ScoopExceptionHandlers scoopExceptionHandlers;


    public EPGUssdPushRoute(CashInLogService cashInLogService, MnoServiceImpl mnoService, DepositService depositService, MainAccountService mainAccountService,
                            VendorService vendorService, PushUssdService pushUssdService,
                            CircuitBreakerService circuitBreakerService,
                            ServiceNameComponent serviceNameComponent,
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
        getContext().getRegistry().bind("halopesaHttpClient", halopesaHttpClient);

        serviceNameComponent.setServiceName(ServiceName.HALOTEL_PAYMENT);

        HalopesaCircuitBreakerProcessor halopesaCircuitProcessor = new HalopesaCircuitBreakerProcessor(
                halopesaCircuitBreaker,
                cashInLogService,
                depositService,
                serviceNameComponent
        );

        //Register Processor
        getContext().getRegistry().bind("halopesaCircuitProcessor", halopesaCircuitProcessor);

        onException(IllegalArgumentException.class)
                .maximumRedeliveries(0) // Don't retry for validation errors
                .handled(true)
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    markRequestAsFailed(exchange);
                    log.error("Validation error: {}", cause.getMessage());
                });

        // Error handling with retries
        onException(Exception.class)
                .maximumRedeliveries(MAX_RETRIES)
                .redeliveryDelay(2000)
                .backOffMultiplier(2)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                // Add retry condition - only retry for retryable exceptions
                .retryWhile(this::shouldRetryCheck)
                .onRedelivery(exchange -> {
                    int retryCount = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, 0, Integer.class);
                    updateCashInLogForRetry(exchange, retryCount);
                    log.warn("Retry attempt {} of {} for transaction: {}",
                            retryCount,
                            MAX_RETRIES,
                            exchange.getIn().getHeader("transactionId"));
                })
                .onExceptionOccurred(exchange -> {
                    int retryCount = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, 0, Integer.class);
                    if (retryCount >= MAX_RETRIES) {
                        markRequestAsFailed(exchange);
                    }
                })
                .handled(true);

        MnoMapping halopesaTanzania = this.mnoService.findMappingByMno("Halopesa-Tanzania");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(halopesaTanzania.getMno());

        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);

        from("quartz://depositInitiation/halopesa?cron=0/30+*+*+*+*+?&stateful=false") // Trigger every 1 second
                .routeId("init-halopesa-deposits-producer")
                .threads().executorService(halopesaThreadPool.getThreadPoolExecutor())
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
                .toD(CamelConfiguration.RABBIT_PRODUCER_HALOPESA_INITS_URI)
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

        // Main route
        from(CamelConfiguration.RABBIT_CONSUMER_HALOPESA_INITS_URI)
                .routeId("init-halopesa-deposits-consumer")
                .log("Received record from RabbitMQ: ${body}")
                .bean(InitDepositDeduplicationService.class, "checkAndMarkInitDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(this::createInitialCashInLog)
                .process("halopesaCircuitProcessor")
                .doTry()
                .process(this::prepareRequest)
                .log("Init Halopesa USSD Push request sent: ${body}")
                .removeHeaders("*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("CamelHttpClient", constant(halopesaHttpClient))
                .toD("${exchangeProperty.networkConfig.apiUrl}")
                .process(exchange -> {
                    // Record successful call to circuit breaker
                    circuitBreakerService.recordSuccess("halopesaPaymentService");
                })
                .choice()
                .when(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo(200))
                .throwException(new RuntimeException("EPG request failed with status: ${header.CamelHttpResponseCode}"))
                .end() // End of choice block
                .log("Halopesa USSD Push Response: ${body}")
                .toD("direct:recordPush")
                .log("Record sent to RabbitMQ: ${body}")
                .endDoTry() // End of try block
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
                    String errorPrefix = "Halopesa payment failed: ";

                    // Only record circuit breaker failure for service-related issues
                    if (!(exception instanceof HalopesaCircuitBreakerProcessor.CircuitBreakerOpenException)
                            && this.scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
                        circuitBreakerService.recordFailure("halopesaPaymentService", exception);
                    }

                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        if (cashInLog != null) {
                            cashInLog.setStatus(status);
                            cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
                            cashInLogService.recordLog(cashInLog);
                        }
                    }

                    // Update deposit transaction
                    String transactionId = exchange.getProperty("transactionId", String.class);
                    if (transactionId != null) {
                        depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                            deposit.setRequestStatus(status);

                            // More descriptive messages based on error type
                            String errorMessage = switch (status) {
                                case NETWORK_CONNECTION_ISSUE ->
                                        "Could not connect to Halopesa servers";
                                case TIMEOUT ->
                                        "Halopesa service did not respond in time";
                                case SERVICE_UNAVAILABLE ->
                                        "Halopesa service is currently unavailable";
                                case INVALID_CREDENTIALS ->
                                        "Authentication failed with Halopesa service";
                                case INVALID_REQUEST ->
                                        "Invalid request format or parameters";
                                case BUSINESS_RULE_VIOLATION ->
                                        "Transaction violates business rules";
                                default ->
                                        "Payment processing failed: " + exception.getMessage();
                            };

                            deposit.setErrorMessage(errorMessage);
                            depositService.recordDeposit(deposit);
                        });
                    }
                })
                .log(LoggingLevel.ERROR, "Network connection issue encountered: ${exception.message}")
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

        from("direct:recordPush")
                .routeId("record-push-consumer")
                .log("Received response from Push: ${body}")
                .process(exchange -> {
                    /*ObjectMapper mapper = new ObjectMapper();*/
                    // Create XML and JSON mappers
                    XmlMapper xmlMapper = new XmlMapper();
                    ObjectMapper jsonMapper = new ObjectMapper();

                    // Convert XML to POJO
                    PaymentGatewayResponse paymentGatewayResponse = xmlMapper.readValue(exchange.getIn().getBody(String.class), PaymentGatewayResponse.class);

                    this.depositService.findByTransactionId(paymentGatewayResponse.getBody().getResponse().getTransactionNumber()).ifPresent(deposit -> {
                        PushUssd pushUssd = PushUssd.builder()
                                .status(paymentGatewayResponse.getBody().getResponse().getResponseCode())
                                .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                .message(paymentGatewayResponse.getBody().getResponse().getResponseStatus())
                                .reference(paymentGatewayResponse.getBody().getResponse().getReference())
                                .currency(deposit.getCurrency())
                                .operator(this.mnoService.searchMno(deposit.getMsisdn()))
                                .transactionNumber(paymentGatewayResponse.getBody().getResponse().getTransactionNumber())
                                .accountId(String.valueOf(mainAccountService.findByVendorDetails(deposit.getVendorDetails()).getId()))
                                .vendorDetails(deposit.getVendorDetails())
                                .sessionId(deposit.getSessionId())
                                .msisdn(deposit.getMsisdn())
                                .build();
                        this.pushUssdService.update(pushUssd);

                        // Update session
                        String sessionId = exchange.getProperty("sessionId", String.class);
                        if (sessionId != null && !paymentGatewayResponse.getBody().getResponse().getResponseCode().equalsIgnoreCase("0")) {
                            updateSession(sessionId, "FAILED", "Payment processing failed");
                            exchange.setProperty("failedMessage", "Payment initiation failed");
                            // Send callback to vendor
                            exchange.getContext().createProducerTemplate().send("direct:halopesa-tanzania-init-vendor-callback", exchange);
                        }
                    });


                })
                .end();


        // Email route for failure notifications
        from("direct:sendFailureEmail")
                .setHeader("To", constant(EMAIL_ADDRESS))
                .setHeader("Subject", simple("EPG Request Failed - TransactionID: ${header.transactionId}"))
                .setBody(simple("EPG request has failed after ${header.retryCount} retries.\n" +
                        "Transaction ID: ${header.transactionId}\n" +
                        "Reference: ${header.reference}\n" +
                        "Error: ${exception.message}"))
                .toD("smtp://smtp.gmail.com:587?username=alerts@lipanasi.co.tz&password=gTLnra@c2");

        // Vendor Callback route
        from("direct:halopesa-tanzania-init-vendor-callback")
                .routeId("halopesa-tanzania-init-vendor-callback-route")
                .log("Processing Halopesa Tanzania vendor callback: ${body}")
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
                        CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_INIT_URI,
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

    private boolean shouldRetryCheck(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean shouldRetry = this.scoopExceptionHandlers.isRetryableException(exception);

        if (!shouldRetry) {
            log.info("Exception {} is non-retryable, skipping retry",
                    exception.getClass().getSimpleName());
            updateStatusForNonRetryableException(exchange, exception);
        }

        return shouldRetry;
    }

    private void createInitialCashInLog(Exchange exchange) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);
            exchange.setProperty("networkConfig", (HaloPesaConfig) this.sessionManagementService.getSession(depositDto.getSessionId()).orElseThrow(()->
                    new RuntimeException("Session not found for ID: " + depositDto.getSessionId())).getNetworkConfig());
            CashInLog cashInLog = CashInLog.builder()
                    .cashInRequest("{'transactionNumber':" + depositDto.getTransactionId() + ",'amount':" + depositDto.getAmount() + ",'currency':" + depositDto.getCurrency() + ",'msisdn':" + depositDto.getMsisdn() + ",'reference':" + depositDto.getPaymentReference() + ",'operatorId':" + depositDto.getOperator() + "}")
                    .retryCount(0)
                    .status(RequestStatus.INITIATED)
                    .paymentReference(depositDto.getPaymentReference())
                    .build();

            cashInLog = cashInLogService.recordLog(cashInLog);
            exchange.setProperty("cashInLogId", cashInLog.getId());
        } catch (Exception e) {
            exchange.setProperty("error", e.getMessage());
        }
    }

    private void updateCashInLogForRetry(Exchange exchange, int retryCount) {
        Long cashInLogId = exchange.getProperty("cashInLogId", Long.class);
        CashInLog cashInLog = cashInLogService.findById(cashInLogId)
                .orElseThrow(() -> new RuntimeException("CashInLog not found"));

        cashInLog.setRetryCount(retryCount);
        cashInLog.setStatus(retryCount < 3 ? RequestStatus.MARKED_FOR_RETRY : RequestStatus.FAILED);
        cashInLogService.recordLog(cashInLog);
    }

    private void prepareRequest(Exchange exchange) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);

        // Validate required fields
        if (depositDto == null) {
            throw new IllegalArgumentException("DepositDto cannot be null");
        }

        // Get and validate input parameters
        String msisdn = validateField(depositDto.getMsisdn(), "MSISDN");
        String amount = validateAmount(Float.parseFloat(String.valueOf(depositDto.getAmount())));
        String reference = validateField(depositDto.getPaymentReference(), "Payment Reference");
        String transactionId = validateField(depositDto.getTransactionId(), "Transaction ID");

        if (depositDto.getVendorDto() == null) {
            throw new IllegalArgumentException("Vendor details cannot be null");
        }

        // Create timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Generate spPassword with validated values
        String rawPassword = validateField("${exchangeProperty.networkConfig.spId}", "SP ID") +
                validateField("${exchangeProperty.networkConfig.secretKey}", "Secret Key") +
                timestamp +
                amount.replaceAll("\\.\\d+", "") +
                msisdn;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawPassword.getBytes());
        String spPassword = Base64.encodeBase64String(hash);

        // Create JSON payload with validated values
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode headerNode = mapper.createObjectNode();
        headerNode.put("spId", "${exchangeProperty.networkConfig.spId}");
        headerNode.put("merchantCode", validateField("${exchangeProperty.networkConfig.merchantCode}", "Merchant Code"));
        headerNode.put("spPassword", spPassword);
        headerNode.put("timestamp", timestamp);
        headerNode.put("apiVersion", "5.0");

        ObjectNode bodyNode = mapper.createObjectNode();
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("command", "UssdPush");
        requestNode.put("command1", "UssdPush");
        requestNode.put("reference", reference);
        requestNode.put("transactionID", transactionId);
        requestNode.put("msisdn", msisdn);
        requestNode.put("amount", amount.replaceAll("\\.\\d+", ""));
        requestNode.put("currency", "TZS");
        requestNode.put("transactionChannel", "MOBAPP");

        bodyNode.set("request", requestNode);
        rootNode.set("header", headerNode);
        rootNode.set("body", bodyNode);

        exchange.getIn().setBody(mapper.writeValueAsString(rootNode));
    }

    private String validateField(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    private String validateAmount(Float amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        return String.valueOf(amount);
    }


    private void markRequestAsFailed(Exchange exchange) {
        DepositDto depositDto = new Gson().fromJson(exchange.getIn().getBody(String.class), DepositDto.class);
        String transactionId = depositDto.getTransactionId();//exchange.getIn().getHeader("transactionId", String.class);
        String reference = depositDto.getPaymentReference();//exchange.getIn().getHeader("reference", String.class);
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Long cashInLogId = exchange.getProperty("cashInLogId", Long.class);

        // Update database
        CashInLog cashInLog = cashInLogService.findById(cashInLogId)
                .orElseThrow(() -> new RuntimeException("CashInLog not found"));
        cashInLog.setStatus(RequestStatus.FAILED);
        cashInLog.setErrorMessage("Payment gateway error: " + cause.getMessage());
        cashInLogService.recordLog(cashInLog);

        // Send email notification
        ProducerTemplate template = exchange.getContext().createProducerTemplate();
        Map<String, Object> headers = new HashMap<>();
        headers.put("transactionId", transactionId);
        headers.put("reference", reference);
        headers.put("retryCount", MAX_RETRIES);
        template.sendBodyAndHeaders("direct:sendFailureEmail", null, headers);

        log.error("Request failed after {} retries. TransactionId: {}, Reference: {}, Error: {}",
                MAX_RETRIES,
                transactionId,
                reference,
                cause.getMessage());
    }


    // Helper method to update status for non-retryable exceptions immediately
    private void updateStatusForNonRetryableException(Exchange exchange, Exception exception) {
        RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
        String errorPrefix = "Halopesa payment failed (non-retryable): ";

        // Update cash_in_logs
        String cashInLogId = exchange.getProperty("cashInLogId", String.class);
        if (cashInLogId != null) {
            CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
            if (cashInLog != null) {
                cashInLog.setStatus(status);
                cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
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
            case NETWORK_CONNECTION_ISSUE ->
                    "Could not connect to Halopesa servers";
            case TIMEOUT ->
                    "Halopesa service did not respond in time";
            case SERVICE_UNAVAILABLE ->
                    responseCode != null && responseCode == 429
                            ? "Too many requests - please try again later"
                            : "Halopesa service is currently unavailable";
            case INVALID_CREDENTIALS ->
                    "Authentication failed with Halopesa service";
            case INVALID_REQUEST ->
                    "Invalid request format or parameters";
            case BUSINESS_RULE_VIOLATION ->
                    "Transaction violates business rules";
            case RESOURCE_NOT_FOUND ->
                    "Requested resource not found";
            default ->
                    "Payment processing failed" + (responseCode != null ? " (HTTP " + responseCode + ")" : "");
        };
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
