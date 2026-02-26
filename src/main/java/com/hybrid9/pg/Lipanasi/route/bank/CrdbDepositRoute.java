package com.hybrid9.pg.Lipanasi.route.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.VatInitialRequestDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.bank.CardPaymentDto;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.BillingInfo;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentResponse;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositRequest;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositResponse;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedCallBack;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.ConstructorBuilder;
import com.hybrid9.pg.Lipanasi.route.processor.bank.BankDepositProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.bank.BankProcessor;
import com.hybrid9.pg.Lipanasi.route.resources.DepositResources;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.payments.TransactionLogServiceImpl;
import com.hybrid9.pg.Lipanasi.services.DeduplicationBankService;
import com.hybrid9.pg.Lipanasi.services.IdempotencyBankService;
import com.hybrid9.pg.Lipanasi.services.bank.CardPaymentService;
import com.hybrid9.pg.Lipanasi.services.payments.FailedCallBackService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.nimbusds.jose.shaded.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CrdbDepositRoute extends RouteBuilder {
    private final TransactionLogServiceImpl transactionLogServiceImpl;

    @Value("${payment-gateway.scoop.deposit-url}")
    private String depositUrl;

    @Value("${payment-gateway.scoop.idempotency.window-seconds:3600}")
    private int idempotencyWindowSeconds;

    @Value("${payment-gateway.scoop.batch-size:50}")
    private int batchSize;

    @Value("${payment-gateway.scoop.processing-timeout-minutes:30}")
    private int processingTimeoutMinutes = 30;

    @Value("${payment-gateway.scoop.dedup-expiry-hours:24}")
    private int dedupExpiryHours = 24;

    @Value("${payment-gateway.scoop.crdb.tps:10}")
    private int crdbTps;

    @Value("${payment-gateway.scoop.crdb.payment.url}")
    private String crdbPaymentUrl;

    @Autowired
    @Qualifier("crdbThreadPool")
    private ThreadPoolTaskExecutor crdbThreadPool;

    @Autowired
    @Qualifier("crdbVirtualThread")
    private ExecutorService crdbVirtualThread;

    @Autowired
    @Qualifier("depositIdempotentRepository")
    private IdempotentRepository idempotentRepository;

    private final CardPaymentService cardPaymentService;
    private final MnoServiceImpl mnoService;
    private final ConstructorBuilder constructorBuilder;
    private final PushUssdRefService pushUssdRefService;
    private final IdempotencyBankService idempotencyService;
    private final DeduplicationBankService deduplicationService;
    public final DepositResources depositResources;
    public final BankDepositProcessor depositProcessor;
    private final SessionManagementService sessionManagementService;
    private final FailedCallBackService failedCallBackService;


    public CrdbDepositRoute(
            CardPaymentService cardPaymentService,
            MnoServiceImpl mnoService,
            ConstructorBuilder constructorBuilder,
            TransactionLogServiceImpl transactionLogServiceImpl,
            PushUssdRefService pushUssdRefService,
            IdempotencyBankService idempotencyService,
            DeduplicationBankService deduplicationService,
            DepositResources depositResources,
            BankDepositProcessor depositProcessor,
            SessionManagementService sessionManagementService,
            FailedCallBackService failedCallBackService) {
        this.pushUssdRefService = pushUssdRefService;
        this.transactionLogServiceImpl = transactionLogServiceImpl;
        this.cardPaymentService = cardPaymentService;
        this.mnoService = mnoService;
        this.constructorBuilder = constructorBuilder;
        this.idempotencyService = idempotencyService;
        this.deduplicationService = deduplicationService;
        this.depositResources = depositResources;
        this.depositProcessor = depositProcessor;
        this.sessionManagementService = sessionManagementService;
        this.failedCallBackService = failedCallBackService;

    }

    @Override
    public void configure() throws Exception {
        Gson gson = new Gson();


        // Configure global error handling
        configureErrorHandling();

        // Get MNO mapping for crdb-Tanzania
       // MnoMapping crdbSettings = this.mnoService.findMappingByMno("CRDB");

        // Configure lists for filtering PushUssd records
        List<String> bankNameList = new ArrayList<>();
        bankNameList.add("CRDB");

        List<CollectionStatus> collectionStatusList = new ArrayList<>();
        collectionStatusList.add(CollectionStatus.NEW);
        collectionStatusList.add(CollectionStatus.FAILED);

        // Producer route - fetch records and send to RabbitMQ
        configureProducerRoute(gson, bankNameList, collectionStatusList);

        // Consumer route - process messages from RabbitMQ
        configureConsumerRoute();

        // Configure route for processing CRDB Tanzania payments
        configurePaymentProcessorRoute();

        // Configure routes for account balance updates and status updates
        configureStatusUpdateRoutes();

        // Configure routes for VAT
        configureVatRoutes();

        // Configure routes for payment processing
        configurePaymentRoute();

        // Configure routes for deposit completion
        completeCardDeposit();

        // Configure routes for async callback
        configureAsyncCallbackRoute();

        // Configure routes for failed deposits
        recordFailedDeposits();
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
                .onRedelivery(exchange -> {
                    int retries = exchange.getIn().getHeader("CamelRedeliveryCounter", 0, Integer.class);
                    String transactionId = exchange.getIn().getHeader("transactionId", String.class);
                    //String mobileMoney = exchange.getIn().getHeader("mobileMoneyName", String.class);
                    log.warn("Retry attempt {} for message {} and bank {}", retries, transactionId, "CRDB");

                    // Update retry count in database
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null) {
                        transactionLogServiceImpl.updateRetryCount(cardPayment);
                    }
                })
                .onExceptionOccurred(exchange -> {
                    int retries = exchange.getIn().getHeader("CamelRedeliveryCounter", 0, Integer.class);
                    if (retries >= 3) {
                        CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                        if (cardPayment != null) {
                            // Update status to failed and clear processing flags
                            transactionLogServiceImpl.updateToFailed(cardPayment);

                            // Keep the completed/duplicate record for deduplication
                            idempotencyService.markRecordAsCompleted(cardPayment, dedupExpiryHours);
                        }
                        log.error("Message processing failed after {} retries. Message moved to failed state.", retries);

                        // Update session
                        String sessionId = exchange.getProperty("sessionId", String.class);
                        if (sessionId != null) {
                            updateSession(sessionId, "FAILED", "Payment processing failed");
                            exchange.setProperty("failedMessage", "Payment processing failed");
                            // Send callback to vendor
                            exchange.getContext().createProducerTemplate().send("direct:crdb-tanzania-record-failed-deposits", exchange);
                        }
                    }
                })
                .handled(true);
    }


    private void configureProducerRoute(Gson gson, List<String> bankNameList, List<CollectionStatus> collectionStatusList) {
        // Determine throttling settings from MNO config or default value
        int tps =  crdbTps;

        from("quartz://deposit/crdbBank?cron=0/1+*+*+*+*+?&stateful=true")
                .routeId("card-payment-crdb-deposits-producer")
                .throttle(tps).timePeriodMillis(1000)
                .threads()
                .executorService(crdbThreadPool.getThreadPoolExecutor())
                .transacted("PROPAGATION_REQUIRED")
                .process(exchange -> {
                    // Fetch a limited batch of records to avoid overloading the system
                    List<CardPayment> records = cardPaymentService.findByCollectionStatusAndBankName(
                            collectionStatusList, bankNameList);

                    if (records != null && !records.isEmpty()) {
                        // Limit batch size
                        if (records.size() > batchSize) {
                            records = records.subList(0, batchSize);
                        }

                        // Pre-filter records that were already processed recently
                        List<CardPayment> filteredRecords = idempotencyService.filterProcessedRecords(
                                records, idempotencyWindowSeconds);

                        if (!filteredRecords.isEmpty()) {
                            // Update status in a batch operation
                            filteredRecords.forEach(record -> {
                                if (record instanceof CardPayment) {
                                    ((CardPayment) record).setCollectionStatus(CollectionStatus.PROCESSING);
                                }
                            });

                            // Use batch update for better performance
                            cardPaymentService.updateAllCollectionStatus(filteredRecords);
                            exchange.getIn().setBody(filteredRecords);

                            // Mark these records as currently being processed
                            idempotencyService.markRecordsAsProcessing(filteredRecords, processingTimeoutMinutes);
                        } else {
                            exchange.getIn().setBody(new ArrayList<>());
                        }
                    } else {
                        exchange.getIn().setBody(new ArrayList<>());
                    }
                })
                .filter(body().isNotNull())
                .filter(simple("${body.size} > 0"))
                .split(body())
                .log(LoggingLevel.DEBUG, "Processing record: ${body}")
                .process(exchange -> {
                    Object body = exchange.getIn().getBody();
                    if (body != null) {
                        BankProcessor.process(exchange, cardPaymentService, gson);

                        // Add idempotency key header
                        ObjectMapper mapper = new ObjectMapper();
                        log.debug("Card Payment Body >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + exchange.getIn().getBody(String.class));
                        CardPaymentDto cardPayment = mapper.readValue(exchange.getIn().getBody(String.class), CardPaymentDto.class);
                        log.debug("Parsed Card Payment >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + cardPayment);
                        if (cardPayment != null) {
                            // Create a unique idempotency key incorporating business attributes
                            String idempotencyKey = "CRDB_" + cardPayment.getId() + "_" +
                                    cardPayment.getPaymentReference() + "_" +
                                    cardPayment.getTransactionId() + "_" +
                                    cardPayment.getAmount();
                            exchange.getIn().setHeader("IdempotencyKey", idempotencyKey);
                        }
                    }
                })
                // Apply idempotent consumer pattern if repository available
                .choice()
                .when(simple("${ref:depositIdempotentRepository} != null"))
                .idempotentConsumer(header("IdempotencyKey"), idempotentRepository)
                .toD(CamelConfiguration.RABBIT_PRODUCER_CRDB_URI)
                .log("Record sent to RabbitMQ: ${body}")
                .endChoice()
                .otherwise()
                .toD(CamelConfiguration.RABBIT_PRODUCER_CRDB_URI)
                .log("Record sent to RabbitMQ without idempotency check: ${body}")
                .end();
    }

    private void configureConsumerRoute() {
        from(CamelConfiguration.RABBIT_CONSUMER_CRDB_URI)
                .routeId("card-payment-crdb-deposits-consumer")
                .log("Received record from RabbitMQ: ${body}")
                // First check with IdempotencyService for faster cache-based deduplication
                .process(this::processDeduplicationWithRedis)
                .filter(body().isNotNull())  // Only proceed if not a duplicate from idempotency check
                // Now check with DeduplicationService for database-level deduplication and mark as processing
                .bean(DeduplicationBankService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate from deduplication check
                .process(this::process)
                .toD("direct:crdb-tanzania-payment-router")
                .end();
    }


    // Bank Payment Route
    private void configurePaymentRoute() {
        from("direct:crdb-tanzania-payment-router")
                .routeId("crdb-tanzania-payment-router")
                .log("Processing crdb-Tanzania Payment: ${body}")
                .doTry()
                .process(exchange -> {
                    log.info("Processing Bank Reference: {}", exchange.getIn().getHeader("paymentReference", String.class));

                    // Double-check idempotency before processing
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null && idempotencyService.isRecordProcessed(cardPayment)) {
                        log.info("[CRDB Payment] Detected duplicate transaction at payment processor stage: ID {}, Reference {}",
                                cardPayment.getId(), cardPayment.getPaymentReference());
                        exchange.setProperty("skipProcessing", true);
                        return;
                    }

                    log.info("[CRDB Payment] Is cardPayment null? {}", cardPayment == null);

                    if (cardPayment == null) {
                        throw new IllegalStateException("cardPayment must not be null at this stage");
                    }


                    // Build payment request object
                    PaymentRequest paymentRequest = PaymentRequest.builder()
                            .transientToken(cardPayment.getCardToken())
                            .amount(String.valueOf(cardPayment.getAmount()))
                            .currency(cardPayment.getCurrency())
                            .orderId(cardPayment.getOriginalReference())
                            .capture(false)
                            .billingInfo(BillingInfo.builder()
                                    .firstName(cardPayment.getBillingInformation().getFirstName())
                                    .lastName(cardPayment.getBillingInformation().getLastName())
                                    .email(cardPayment.getBillingInformation().getEmail())
                                    .phone(cardPayment.getBillingInformation().getPhone())
                                    .address1(cardPayment.getBillingInformation().getAddress1())
                                    .city(cardPayment.getBillingInformation().getCity())
                                    .state(cardPayment.getBillingInformation().getState())
                                    .zipCode(cardPayment.getBillingInformation().getPostalCode())
                                    .country(cardPayment.getBillingInformation().getCountry())
                                    .email(cardPayment.getBillingInformation().getEmail())
                                    .phone(cardPayment.getBillingInformation().getPhone())
                                    .build())
                            .build();

                    exchange.setProperty("paymentRequest", paymentRequest);

                    //exchange.setProperty("depositRequest", paymentRequest);

                    // Convert deposit request to JSON
                    ObjectMapper mapper = new ObjectMapper();
                    String apiJsonRq = mapper.writeValueAsString(paymentRequest);
                    log.debug("[CRDB Payment] API payload: {}", apiJsonRq);

                    exchange.getIn().setBody(apiJsonRq);
                })
                // Check early exit flag and skip further processing if set
                .choice()
                .when(simple("${exchangeProperty.skipProcessing} == true"))
                .log("Skipping duplicate transaction processing")
                .endChoice()
                .otherwise()
                // Set HTTP headers
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("CRDB [Scoop] Payment Request Body: ${body}")
                // Make HTTP call to deposit URL
                .toD(crdbPaymentUrl + "?bridgeEndpoint=true&httpMethod=POST&connectTimeout=5000&socketTimeout=10000")
                .log("CRDB [Scoop] Payment Response: ${body}")
                // Process response
                .process(exchange -> {
                    String responseMessage = exchange.getIn().getBody(String.class);
                    ObjectMapper mapper = new ObjectMapper();
                    PaymentResponse paymentResponse = mapper.readValue(responseMessage, PaymentResponse.class);

                    log.info("Payment Response: {}", responseMessage);

                    this.depositProcessor.recordPaymentResponse(paymentResponse, exchange.getIn().getHeader("cardPayment", CardPayment.class))
                            .thenAccept(cardPayment -> {
                                log.debug(">>>>>>>>>>>>>>>Updated card payment: {}", cardPayment);
                                // set exchange property - updated card payment
                                exchange.setProperty("updatedCardPayment", cardPayment);

                                // Complete the deposit
                                exchange.getContext().createProducerTemplate().send("direct:crdb-tanzania-payment-processor", exchange);
                            }).exceptionally(e -> {
                                log.error("Error completing deposit for Reference: {}", exchange.getProperty("paymentReference", String.class), e);
                                return null;
                            });
                })
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

    // Bank Payment Processing Route
    private void configurePaymentProcessorRoute() {
        // Determine throttling settings from MNO config or default value
        int tps =  crdbTps;

        // Payment processing route
        from("direct:crdb-tanzania-payment-processor")
                .threads().executorService(crdbVirtualThread)
                .throttle(tps).timePeriodMillis(1000) // Dynamic TPS based on MNO config
                .routeId("crdb-tanzania-payment-processor")
                .log("Processing crdb-Tanzania Payment: ${body}")
                .doTry()
                .process(exchange -> {
                    // Store original body for later restoration
                    exchange.setProperty("originalJsonBody", exchange.getIn().getBody());
                })
                .choice()
                // Case 1: Both commission and VAT enabled
                .when(simple("${exchangeProperty.vendorDto.hasCommission} == 'true' && ${exchangeProperty.vendorDto.hasVat} == 'true' && ${exchangeProperty.cardStatus} == '0'"))
                .log("Processing both Commission and VAT")
                .process(this::processCommissionAndVatSync)
                // Case 2: Only commission enabled, no VAT
                .when(simple("${exchangeProperty.vendorDto.hasCommission} == 'true' && ${exchangeProperty.vendorDto.hasVat} != 'true' && ${exchangeProperty.cardStatus} == '0'"))
                .log("Processing Commission only")
                .process(this::processCommissionSync)
                // Case 3: Only VAT enabled, no commission
                .when(simple("${exchangeProperty.vendorDto.hasCommission} != 'true' && ${exchangeProperty.vendorDto.hasVat} == 'true' && ${exchangeProperty.cardStatus} == '0'"))
                .log("Processing VAT only")
                .process(this::processVatSync)
                // Case 4: Neither commission nor VAT enabled
                .otherwise()
                .log("No Commission or VAT processing required")
                .toD("direct:crdb-complete-card-deposit")
                .endChoice()
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

    // Bank Deposit Route
    public void completeCardDeposit() {
        from("direct:crdb-complete-card-deposit")
                .routeId("crdb-complete-card-deposit-route")
                .log("Processing crdb-complete-card-deposit: ${body}")
                .doTry()
                .process(exchange -> {
                    // Restore original body for deposit processing
                    Object originalBody = exchange.getProperty("originalJsonBody");
                    if (originalBody != null) {
                        exchange.getIn().setBody(originalBody);
                    }
                })
                .process(exchange -> {
                    log.info("Processing CRDB Reference: {}", exchange.getProperty("paymentReference", String.class));
                    log.debug(">>>>>>>>>>>>>>>Updated Card Payment: {}", exchange.getProperty("updatedCardPayment", CardPayment.class));
                    // Double-check idempotency before processing
                    CardPayment cardPayment = exchange.getProperty("updatedCardPayment", CardPayment.class);
                    if (cardPayment != null && idempotencyService.isRecordProcessed(cardPayment)) {
                        log.info("[CRDB Deposit] Detected duplicate transaction at payment processor stage: ID {}, Reference {}",
                                cardPayment.getId(), cardPayment.getPaymentReference());
                        exchange.setProperty("skipProcessing", true);
                        return;
                    }

                    log.info("[CRDB Deposit] Is cardPayment null? {}", cardPayment == null);

                    if (cardPayment == null) {
                        throw new IllegalStateException("cardPayment must not be null at this stage");
                    }

                    // Initialize deposit with required parameters
                    try {
                        this.depositProcessor.initDeposit(cardPayment).join();
                        log.info("Deposit initialized successfully for Reference: {}", exchange.getProperty("paymentReference", String.class));
                    } catch (Exception e) {
                        log.error("Error initializing deposit for Reference: {}", exchange.getProperty("paymentReference", String.class), e);
                        throw new RuntimeException("Deposit initialization failed", e);
                    }



                    // Build deposit request object
                    DepositRequest depositRequest = DepositRequest.builder()
                            .transactionNo(cardPayment.getOriginalReference())
                            .reference(this.depositResources.extractOriginalReference(exchange).join())
                            .message(exchange.getIn().getHeader("message", String.class))
                            .status(cardPayment.getStatus().equalsIgnoreCase("0") ? "SUCCESS" : "FAILED")
                            .paymentSessionId(exchange.getProperty("sessionId", String.class))
                            .build();

                    exchange.setProperty("depositRequest", depositRequest);

                    // Convert deposit request to JSON
                    ObjectMapper mapper = new ObjectMapper();
                    String apiJsonRq = mapper.writeValueAsString(depositRequest);
                    log.info("[CRDB Deposit] API payload: {}", apiJsonRq);

                    exchange.getIn().setBody(apiJsonRq);
                })
                // Check early exit flag and skip further processing if set
                .choice()
                .when(simple("${exchangeProperty.skipProcessing} == true"))
                .log("Skipping duplicate transaction processing")
                .endChoice()
                .otherwise()
                // Set HTTP headers
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("x-api-key", constant("cd3f2dee-b4c0-47b7-b71c-c35b2c55e8b1"))
                .setHeader("Idempotency-Key", simple("${header.cardPayment.id}-${header.cardPayment.paymentReference}-${header.cardPayment.originalReference}-${date:now:yyyyMMddHHmmss}"))
                .log("CRDB [Scoop] Deposit Request Body: ${body}")
                // Make HTTP call to deposit URL
                /*.toD("${exchangeProperty.callbackUrl}?bridgeEndpoint=true&httpMethod=POST&connectTimeout=5000&socketTimeout=10000")
                .log("AirtelMoney [Scoop] Deposit Response: ${body}")
                // Process response
                .process(exchange -> {
                    String responseMessage = exchange.getIn().getBody(String.class);
                    ObjectMapper mapper = new ObjectMapper();

                    // JsonNode used to Skip Modification of DepositResponse
                    JsonNode responseNode = mapper.readTree(responseMessage);
                    DepositResponse depositResponse = DepositResponse.builder()
                            .status(responseNode.path("status").asText())
                            .message(responseNode.path("message").asText())
                            .reference(responseNode.path("reference").asText())
                            .build();

                    log.info("Deposit Response: {}", depositResponse);
                    this.depositProcessor.completeDeposit(depositResponse, exchange.getIn().getHeader("cardPayment", CardPayment.class))
                            .thenRun(() -> log.info("Deposit completed successfully for Reference: {}", exchange.getProperty("paymentReference", String.class)))
                            .exceptionally(e -> {
                                log.error("Error completing deposit for Reference: {}", exchange.getProperty("paymentReference", String.class), e);
                                return null;
                            });
                })*/

                // Using wireTap for fire-and-forget async callback
                .wireTap("direct:crdb-tanzania-async-callback")
                .process(exchange -> {
                    // Create immediate successful response for main flow
                    DepositResponse immediateResponse = DepositResponse.builder()
                            .status("SUCCESS")
                            .message("Deposit processed successfully")
                            .reference(exchange.getProperty("depositRequest", DepositRequest.class).getReference())
                            .build();

                    ObjectMapper mapper = new ObjectMapper();
                    String responseJson = mapper.writeValueAsString(immediateResponse);
                    exchange.getIn().setBody(responseJson);
                    log.info("CRDB [Scoop] Deposit Response (immediate): {}", responseJson);
                })

                // Process response immediately (don't wait for callback)
                .process(exchange -> {
                    String responseMessage = exchange.getIn().getBody(String.class);
                    ObjectMapper mapper = new ObjectMapper();

                    JsonNode responseNode = mapper.readTree(responseMessage);
                    DepositResponse depositResponse = DepositResponse.builder()
                            .status(responseNode.path("status").asText())
                            .message(responseNode.path("message").asText())
                            .reference(responseNode.path("reference").asText())
                            .build();

                    log.info("Deposit Response: {}", depositResponse);

                    // FIXED: Wait for deposit completion before proceeding
                    try {
                        this.depositProcessor.completeDeposit(depositResponse, exchange.getIn().getHeader("cardPayment", CardPayment.class)).join();
                        log.info("Deposit completed successfully for Reference: {}", exchange.getProperty("paymentReference", String.class));
                    } catch (Exception e) {
                        log.error("Error completing deposit for Reference: {}", exchange.getIn().getHeader("paymentReference", String.class), e);
                        throw new RuntimeException("Deposit completion failed", e);
                    }
                })
                .choice()
                .when(simple("${exchangeProperty.cardStatus} == '-1'"))
                .log("Card status is -1, skipping update account balance")
                .toD("direct:crdb-update-card-payment-status")
                .when(simple("${exchangeProperty.cardStatus} == '0'"))
                .log("Card status is 0, updating account balance")
                .toD("direct:crdb-update-account-balance")
                .otherwise()
                .log("Card status is neither -1 nor 0: ${exchangeProperty.cardStatus}")
                .end()
                .endChoice()
                .endDoTry()
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error on complete-card-deposit: ", e);
                    throw e;
                })
                .end();
    }

    // Fire-and-forget async callback using wireTap
    private void configureAsyncCallbackRoute() {
        from("direct:crdb-tanzania-async-callback")
                .routeId("crdb-tanzania-async-callback-processor")
                .threads().executorService(crdbVirtualThread)
                .log("Processing async callback: ${body}")
                .doTry()
                .toD("${exchangeProperty.callbackUrl}?bridgeEndpoint=true&httpMethod=POST&connectTimeout=5000&socketTimeout=10000")
                .log("Async callback successful: ${body}")
                .process(exchange -> {
                    // Optionally log successful callback
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null) {
                        log.info("Callback successful for transaction ID: {}, Reference: {}",
                                cardPayment.getId(), cardPayment.getPaymentReference());
                    }
                })
                .doCatch(org.apache.camel.http.base.HttpOperationFailedException.class)
                .log(LoggingLevel.WARN, "Async callback HTTP error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null) {
                        this.logCallbackFailure(cardPayment, "HTTP Error: " + e.getMessage()).join();
                    }
                })
                .doCatch(java.net.ConnectException.class, java.net.SocketTimeoutException.class)
                .log(LoggingLevel.WARN, "Async callback connection/timeout error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null) {
                        this.logCallbackFailure(cardPayment, "Connection/Timeout Error: " + e.getMessage()).join();
                    }
                })
                .doCatch(Exception.class)
                .log(LoggingLevel.WARN, "Async callback general error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null) {
                        this.logCallbackFailure(cardPayment, "General Error: " + e.getMessage()).join();
                    }
                })
                .end();
    }

    private void recordFailedDeposits() {
        // Vendor Callback route
        from("direct:crdb-tanzania-record-failed-deposits")
                .routeId("crdb-tanzania-record-failed-deposits-route")
                .log("Processing vendor callback: ${body}")
                .doTry()
                .process(exchange -> {
                    // Restore original body for later restoration
                    exchange.setProperty("originalJsonBody", exchange.getIn().getBody());
                })
                .process(exchange -> {
                    log.info("Processing Vendor Callback: " + exchange.getIn().getBody(String.class));
                    // Prepare vendor callback
                    this.processFailedDeposit(exchange, exchange.getProperty("sessionId", String.class));
                })
                .toD(CamelConfiguration.RABBIT_PRODUCER_FAILED_DEPOSITS_URI)
                .log("Recorded vendor callback: ${body}")
                .setBody(simple("${exchangeProperty.originalJsonBody}"))
                /*.process(exchange -> {
                    // Acknowledge the message
                    Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
                    Long deliveryTag = exchange.getIn().getHeader(SpringRabbitMQConstants.DELIVERY_TAG, Long.class);
                    if (channel != null && deliveryTag != null) {
                        channel.basicAck(deliveryTag, false);
                    }
                })*/
                .endDoTry() // End of try block
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing transaction after ${header.CamelRedeliveryCounter} attempts: ${exception.message}")
                .end()
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .end();
    }

    private void processFailedDeposit(Exchange exchange, String sessionId) {
        AtomicReference<CardPayment> cardPaymentReference = new AtomicReference<>();
        this.cardPaymentService.findBySessionId(sessionId).ifPresent(cardPaymentReference::set);
        // Build deposit request object
        FailedDepositRequest depositRequest = FailedDepositRequest.builder()
                .transactionNo(cardPaymentReference.get().getTransactionId())
                .reference(cardPaymentReference.get().getPaymentReference())
                .message(exchange.getProperty("failedMessage", String.class))
                .status("FAILED")
                .channel("CARD")
                .paymentSessionId(exchange.getProperty("sessionId", String.class))
                .callbackUrl(cardPaymentReference.get().getVendorDetails().getCallbackUrl())
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

    private CompletableFuture<Void> logCallbackFailure(CardPayment cardPayment, String callbackError) {
        // Add callback failure to database

        FailedCallBack failedCallBack = FailedCallBack.builder()
                .reference(cardPayment.getPaymentReference())
                .originalReference(cardPayment.getOriginalReference())
                .errorMessage(callbackError)
                .cardPayment(cardPayment)
                .build();

        // Save to database asynchronously
        return CompletableFuture.runAsync(() -> failedCallBackService.createFailedCallBack(failedCallBack), crdbVirtualThread);

    }

    private void processCommissionAndVatSync(Exchange exchange) {
        VendorDto vendorDto = exchange.getProperty("vendorDto", VendorDto.class);
        String sessionId = exchange.getProperty("sessionId", String.class);
        BigDecimal amount = BigDecimal.valueOf(exchange.getIn().getHeader("amount", Float.class));
        String paymentReference = exchange.getProperty("paymentReference",String.class);
        // String msisdn = exchange.getIn().getHeader("msisdn", String.class);
        String collectionType = exchange.getIn().getHeader("collectionType", String.class);
        CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
        log.debug(">>>>>>>>>>>>>>>>>>>> Processing commission and VAT for sessionId: " + sessionId);
        try {
            // Process commission synchronously
            this.depositResources.processBankCommission(vendorDto, sessionId, amount, null, collectionType,"CRDB", paymentReference,null,cardPayment).join();
            log.info("Commission processed successfully for sessionId: {}", sessionId);

            // Process VAT synchronously
            Exchange vatExchange = exchange.getContext().createProducerTemplate().send("direct:process-vat", exchange);

            // Ensure the exchange continues with the original state
            exchange.getIn().setBody(vatExchange.getIn().getBody());
            //exchange.setProperty(vatExchange.getProperties());
            log.info("VAT processed successfully for sessionId: {}", sessionId);

            // Continue processing deposits
            exchange.getContext().createProducerTemplate().send("direct:crdb-complete-card-deposit", exchange);

        } catch (Exception e) {
            log.error("Error processing commission and VAT for sessionId: {}", sessionId, e);
            throw new RuntimeException("Commission and VAT processing failed", e);
        }
    }

    private void configureVatRoutes() {
        from("direct:process-vat")
                .id("process-vat-route")
                .setProperty("originalBody", simple("${body}"))
                .log(LoggingLevel.INFO, "[Scoop] Processing VAT")
                //.bean(depositResources, "processVat(${exchangeProperty.vendorDto},${exchangeProperty.sessionId},${exchangeProperty.amount},${exchangeProperty.msisdn},${exchangeProperty.collectionType},${exchangeProperty.paymentReference})")
                .process(exchange -> {
                    VendorDto vendorDto = exchange.getIn().getHeader("vendorDto", VendorDto.class);

                    VatInitialRequestDto vatInitialRequestDto = VatInitialRequestDto.builder()
                            .vendorExternalId(vendorDto != null ? vendorDto.getExternalId() : null)
                            .sessionId(exchange.getProperty("sessionId", String.class))
                            .amount(exchange.getIn().getHeader("amount", Float.class))
                            .msisdn(exchange.getIn().getHeader("msisdn", String.class))
                            .collectionType(exchange.getIn().getHeader("collectionType", String.class))
                            .paymentReference(exchange.getProperty("paymentReference", String.class))
                            .build();

                    // Convert to JSON explicitly
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonBody = mapper.writeValueAsString(vatInitialRequestDto);

                    exchange.getMessage().setBody(jsonBody);
                })
                .to(CamelConfiguration.RABBIT_PRODUCER_VAT_URI)
                .log(LoggingLevel.INFO, "VAT processed")
                // Restore the original body for continuation
                .setBody(simple("${exchangeProperty.originalBody}"))
                .end();
    }

    private void processVatSync(Exchange exchange) {
        try {
            exchange.getContext().createProducerTemplate().send("direct:process-vat", exchange);
            log.info("VAT processed successfully for sessionId: {}", exchange.getProperty("sessionId", String.class));

            // Continue processing deposits
            exchange.getContext().createProducerTemplate().send("direct:crdb-complete-card-deposit", exchange);
        } catch (Exception e) {
            log.error("Error processing VAT for sessionId: {}", exchange.getProperty("sessionId", String.class), e);
            throw new RuntimeException("VAT processing failed", e);
        }
    }

    private void processCommissionSync(Exchange exchange) {
        VendorDto vendorDto = exchange.getProperty("vendorDto", VendorDto.class);
        String sessionId = exchange.getProperty("sessionId", String.class);
        BigDecimal amount = BigDecimal.valueOf(exchange.getIn().getHeader("amount", Float.class));
        String paymentReference = exchange.getProperty("paymentReference",String.class);
        // String msisdn = exchange.getIn().getHeader("msisdn", String.class);
        String collectionType = exchange.getIn().getHeader("collectionType", String.class);
        CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);

        try {
            this.depositResources.processBankCommission(vendorDto, sessionId, amount, null, collectionType,"CRDB", paymentReference,null,cardPayment).join();
            log.info("Commission processed successfully for sessionId: {}", sessionId);

            // Continue processing deposits
            exchange.getContext().createProducerTemplate().send("direct:crdb-complete-card-deposit", exchange);
        } catch (Exception e) {
            log.error("Error processing commission for sessionId: {}", sessionId, e);
            throw new RuntimeException("Commission processing failed", e);
        }
    }


    private void configureStatusUpdateRoutes() {
        // Update account balance route
        from("direct:crdb-update-account-balance")
                .threads().executorService(crdbVirtualThread)
                .routeId("crdb-update-account-balance")
                .log("Processing update-account-balance: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    this.depositProcessor.updateDepositBalance(exchange.getIn().getHeader("cardPayment", CardPayment.class))
                            .thenAcceptAsync(result -> {
                                if (result) {
                                    exchange.getContext().createProducerTemplate()
                                            .send("direct:crdb-update-card-payment-status", exchange);
                                }
                            }).exceptionally(e -> {
                                log.error("Error processing update-account-balance", e);
                                throw new RuntimeException("Failed to update account balance", e);
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

        // Update USSD push status route
        from("direct:crdb-update-card-payment-status")
                .routeId("crdb-update-card-payment-status")
                .log("Processing update-card-payment-status: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    this.depositProcessor.updateCardPaymentStatusAsync(exchange.getIn().getHeader("cardPayment", CardPayment.class))
                            .thenAcceptAsync(success -> {
                                if (success) {
                                    log.info("[Scoop] Card Payment status updated successfully");

                                    exchange.getContext().createProducerTemplate()
                                            .send("direct:crdb-update-deposit-txn-status", exchange);
                                }
                            });
                })
                //.toD("direct:crdb-update-deposit-txn-status")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    throw e;
                })
                .end();

        // Update deposit transaction status route
        from("direct:crdb-update-deposit-txn-status")
                .routeId("crdb-update-deposit-txn-status")
                .log("Processing update-deposit-txn-status: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    this.depositProcessor.updateDepositTransactionStatusAsync(exchange.getIn().getHeader("cardPayment", CardPayment.class))
                            .thenAcceptAsync(success -> {
                                if (success) {
                                    log.info("[Scoop] Deposit transaction status updated successfully");
                                }
                            });

                    // Mark record as completed in idempotency service
                    CardPayment cardPayment = exchange.getIn().getHeader("cardPayment", CardPayment.class);
                    if (cardPayment != null) {
                        // Use the full expiry time when transaction completes successfully
                        idempotencyService.markRecordAsCompleted(cardPayment, dedupExpiryHours);
                    }
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

    private void process(Exchange exchange) {
        ObjectMapper mapper = new ObjectMapper();
        CardPaymentDto cardPaymentDto = null;
        try {
            cardPaymentDto = mapper.readValue(exchange.getIn().getBody(String.class), CardPaymentDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        AtomicLong accountId = new AtomicLong(0L);
        AtomicReference<CardPayment> cardPayment = new AtomicReference<>();

        this.cardPaymentService.findPushUssdById(cardPaymentDto.getId()).ifPresent(cardPayment1 -> {
            //accountId.set(Long.parseLong(cardPayment1.getAccountId()));
            cardPayment.set(cardPayment1);

            // Record this processing in idempotency service as early as possible
            idempotencyService.markRecordAsProcessing(cardPayment1, processingTimeoutMinutes);
        });

        //String mobileMoneyName = this.mnoService.searchMno(pushUssdDto.getMsisdn());

        //exchange.getIn().setHeader("mobileMoneyName", mobileMoneyName);
        //exchange.getIn().setHeader("message", CardPaymentDto.getMessage());
        //exchange.getIn().setHeader("msisdn", pushUssdDto.getMsisdn());
        //exchange.getIn().setHeader("pushUssdId", pushUssdDto.getId());
        //exchange.getIn().setHeader("mainAccountId", accountId.get());
        exchange.setProperty("sessionId", cardPayment.get().getSessionId());
        exchange.setProperty("callbackUrl", cardPayment.get().getVendorDetails().getCallbackUrl());
        exchange.getIn().setHeader("amount", cardPaymentDto.getAmount());
        exchange.getIn().setHeader("collectionType", cardPaymentDto.getCollectionType());
        exchange.setProperty("paymentReference", cardPaymentDto.getPaymentReference());
        exchange.getIn().setHeader("cardPayment", cardPayment.get());
        exchange.setProperty("vendorDto", cardPaymentDto.getVendorDto());
        exchange.setProperty("cardStatus", cardPayment.get().getStatus());
    }

    private void processDeduplicationWithRedis(Exchange exchange) {
        String messageBody = exchange.getIn().getBody(String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            CardPaymentDto dto = mapper.readValue(messageBody, CardPaymentDto.class);

            boolean isDuplicate = false;
            // Check idempotency service first (Redis-based fast check)
            if (dto != null && dto.getId() != null) {
                // Option 1: Check by ID
                isDuplicate = idempotencyService.isRecordProcessed(dto.getId().toString());

                // Option 2: If not found by ID, reconstruct a PushUssd for composite key check
                if (!isDuplicate) {
                    CardPayment tempRecord = new CardPayment();
                    tempRecord.setId(dto.getId());
                    tempRecord.setPaymentReference(dto.getPaymentReference());
                    tempRecord.setTransactionId(dto.getTransactionId());
                    tempRecord.setBankId(dto.getBankId());
                    tempRecord.setAmount(dto.getAmount());

                    isDuplicate = idempotencyService.isRecordProcessed(tempRecord);
                }
            }

            if (isDuplicate) {
                log.info("Idempotency check detected duplicate message (fast path): {}", dto != null ? dto.getPaymentReference() : "unknown");
                exchange.getIn().setBody(null); // Mark for filtering
            } else {
                // If not detected as duplicate by IdempotencyService, proceed to deduplication service
                exchange.getIn().setBody(messageBody);
            }
        } catch (Exception e) {
            log.error("Error during idempotency check", e);
            exchange.getIn().setBody(messageBody); // Proceed with original message
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
