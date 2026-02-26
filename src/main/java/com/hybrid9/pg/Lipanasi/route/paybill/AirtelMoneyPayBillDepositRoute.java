package com.hybrid9.pg.Lipanasi.route.paybill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.PayBillPaymentDto;
import com.hybrid9.pg.Lipanasi.dto.VatInitialRequestDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositRequest;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositResponse;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedCallBack;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.ConstructorBuilder;
import com.hybrid9.pg.Lipanasi.route.processor.PayBillDepositProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.PayBillProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.RabbitMqAckProcessor;
import com.hybrid9.pg.Lipanasi.route.resources.DepositResources;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.payments.TransactionLogServiceImpl;
import com.hybrid9.pg.Lipanasi.services.DeduplicationService;
import com.hybrid9.pg.Lipanasi.services.IdempotencyPayBillService;
import com.hybrid9.pg.Lipanasi.services.payments.FailedCallBackService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
//@Component
public class AirtelMoneyPayBillDepositRoute extends RouteBuilder {
    private final TransactionLogServiceImpl transactionLogServiceImpl;

    @Value("${payment-gateway.scoop.deposit-url}")
    private String depositUrl;

    @Value("${payment-gateway.scoop.idempotency.window-seconds:3600}")
    private int idempotencyWindowSeconds;

    @Value("${payment-gateway.scoop.batch-size:50}")
    private int batchSize;

    @Value("${payment-gateway.scoop.processing-timeout-minutes:30}")
    private int processingTimeoutMinutes;

    @Value("${payment-gateway.scoop.dedup-expiry-hours:24}")
    private int dedupExpiryHours;

    @Value("${payment-gateway.scoop.tps:10}")
    private int airtelTps;

    @Autowired
    @Qualifier("airtelMoneyDepositThreadPool")
    private ThreadPoolTaskExecutor airtelMoneyDepositThreadPool;

    @Autowired
    @Qualifier("airtelVirtualThread")
    private ExecutorService airtelVirtualThread;

    @Autowired
    @Qualifier("depositIdempotentRepository")
    private IdempotentRepository idempotentRepository;

    @Autowired
    @Qualifier("airtelMoneyRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    private final PayBillPaymentService payBillPaymentService;
    private final MnoServiceImpl mnoService;
    private final ConstructorBuilder constructorBuilder;
    private final PushUssdRefService pushUssdRefService;
    private final IdempotencyPayBillService idempotencyService;
    private final DeduplicationService deduplicationService;
    public final DepositResources depositResources;
    public final PayBillDepositProcessor depositProcessor;
    private final SessionManagementService sessionManagementService;
    private final FailedCallBackService failedCallBackService;


    public AirtelMoneyPayBillDepositRoute(
            PayBillPaymentService payBillPaymentService,
            MnoServiceImpl mnoService,
            ConstructorBuilder constructorBuilder,
            TransactionLogServiceImpl transactionLogServiceImpl,
            PushUssdRefService pushUssdRefService,
            IdempotencyPayBillService idempotencyService,
            DeduplicationService deduplicationService,
            DepositResources depositResources,
            PayBillDepositProcessor depositProcessor,
            SessionManagementService sessionManagementService,
            FailedCallBackService failedCallBackService) {
        this.payBillPaymentService = payBillPaymentService;
        this.transactionLogServiceImpl = transactionLogServiceImpl;
        this.pushUssdRefService = pushUssdRefService;
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

        // Get MNO mapping for AirtelMoney-Tanzania
        MnoMapping airtelmoneyTanzania = this.mnoService.findMappingByMno("AirtelMoney-Tanzania");

        // Configure lists for filtering PayBillPayment records
        List<String> airtelmoneyMnoList = new ArrayList<>();
        airtelmoneyMnoList.add(airtelmoneyTanzania.getMno());

        List<CollectionStatus> collectionStatusList = new ArrayList<>();
        collectionStatusList.add(CollectionStatus.COLLECTED);
        collectionStatusList.add(CollectionStatus.FAILED);

        // Producer route - fetch records and send to RabbitMQ
        configureProducerRoute(gson, airtelmoneyMnoList, collectionStatusList, airtelmoneyTanzania);

        // Consumer route - process messages from RabbitMQ
        configureConsumerRoute(airtelmoneyTanzania);

        // Configure route for processing Airtel Money Tanzania payments
        configurePaymentProcessorRoute(airtelmoneyTanzania);

        // Configure routes for account balance updates and status updates
        configureStatusUpdateRoutes();

        // Configure routes for VAT
        configureVatRoutes();

        // Configure routes for deposit processing
        configureDepositRoute();

        // Configure routes for vendor callback
        recordFailedDeposits();

        // Configure routes for async callback
        configureAsyncCallbackRoute();
    }

    private void configureErrorHandling() {
        onException(com.rabbitmq.client.ShutdownSignalException.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .handled(true)
                .process(exchange -> {
                    // Reject message on connection errors
                    exchange.getContext().getRegistry()
                            .lookupByNameAndType("rabbitMqAckProcessor", RabbitMqAckProcessor.class)
                            .rejectWithRequeue(exchange, true);
                })
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
                    String msisdn = exchange.getIn().getHeader("msisdn", String.class);
                    String mobileMoney = exchange.getIn().getHeader("mobileMoneyName", String.class);
                    log.warn("Retry attempt {} for message {} and mobile money {}", retries, msisdn, mobileMoney);

                    // Update retry count in database
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null) {
                        transactionLogServiceImpl.updateRetryCount(PayBillPayment);
                    }
                })
                .onExceptionOccurred(exchange -> {
                    int retries = exchange.getIn().getHeader("CamelRedeliveryCounter", 0, Integer.class);
                    if (retries >= 3) {
                        // Final failure - don't requeue
                        exchange.getContext().getRegistry()
                                .lookupByNameAndType("rabbitMqAckProcessor", RabbitMqAckProcessor.class)
                                .rejectWithRequeue(exchange, false);

                        PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                        if (PayBillPayment != null) {
                            // Update status to failed and clear processing flags
                            transactionLogServiceImpl.updateToFailed(PayBillPayment);

                            // Keep the completed/duplicate record for deduplication
                            idempotencyService.markRecordAsCompleted(PayBillPayment, dedupExpiryHours);
                        }
                        log.error("Message processing failed after {} retries. Message moved to failed state.", retries);

                        // Update session
                        String sessionId = exchange.getProperty("sessionId", String.class);
                        if (sessionId != null) {
                            updateSession(sessionId, "FAILED", "Payment processing failed");
                            exchange.setProperty("failedMessage", "Payment processing failed");
                            // Send callback to vendor
                            exchange.getContext().createProducerTemplate().send("direct:airtel-tanzania-record-failed-deposits", exchange);
                        }
                    }
                })
                .handled(true);
    }


    private void configureProducerRoute(Gson gson, List<String> airtelmoneyMnoList, List<CollectionStatus> collectionStatusList, MnoMapping airtelmoneyTanzania) {
        // Determine throttling settings from MNO config or default value
        int tps = airtelmoneyTanzania.getTps() > 0 ? airtelmoneyTanzania.getTps() : airtelTps;

        from("quartz://deposit/airtelMoney?cron=0/1+*+*+*+*+?&stateful=true")
                .routeId("ussd-airtelmoney-deposits-producer")
                .throttle(tps).timePeriodMillis(1000)
                .threads()
                .executorService(airtelMoneyDepositThreadPool.getThreadPoolExecutor())
                .transacted("PROPAGATION_REQUIRED")
                .process(exchange -> {
                    // Enable publisher confirms
                    Channel channel = exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL, Channel.class);
                    if (channel != null) {
                        channel.confirmSelect();
                        exchange.setProperty("rabbitmqChannel", channel);
                    }
                })
                .process(exchange -> {
                    // Fetch a limited batch of records to avoid overloading the system
                    List<PayBillPayment> records = payBillPaymentService.findByCollectionStatusAndOperator(
                            collectionStatusList, airtelmoneyMnoList);

                    if (records != null && !records.isEmpty()) {
                        // Limit batch size
                        if (records.size() > batchSize) {
                            records = records.subList(0, batchSize);
                        }

                        // Pre-filter records that were already processed recently
                        List<PayBillPayment> filteredRecords = idempotencyService.filterProcessedRecords(
                                records, idempotencyWindowSeconds);

                        if (!filteredRecords.isEmpty()) {
                            // Update status in a batch operation
                            filteredRecords.forEach(record -> {
                                if (record instanceof PayBillPayment) {
                                    ((PayBillPayment) record).setCollectionStatus(CollectionStatus.PROCESSING);
                                }
                            });

                            // Use batch update for better performance
                            payBillPaymentService.updateAllCollectionStatus(filteredRecords);
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
                        PayBillProcessor.process(exchange);

                        // Add idempotency key header
                        ObjectMapper mapper = new ObjectMapper();
                        PayBillPaymentDto PayBillPayment = mapper.readValue(exchange.getIn().getBody(String.class), PayBillPaymentDto.class);
                        if (PayBillPayment != null) {
                            // Create a unique idempotency key incorporating business attributes
                            String idempotencyKey = "AIRTEL_" + PayBillPayment.getId() + "_" +
                                    PayBillPayment.getPaymentReference() + "_" +
                                    PayBillPayment.getMsisdn() + "_" +
                                    PayBillPayment.getAmount();
                            exchange.getIn().setHeader("IdempotencyKey", idempotencyKey);

                            // set local SessionId
                            exchange.setProperty("localSessionId", PayBillPayment.getSessionId());
                        }


                    }
                })
                // Apply idempotent consumer pattern if repository available
                .choice()
                .when(simple("${ref:depositIdempotentRepository} != null"))
                .idempotentConsumer(header("IdempotencyKey"), idempotentRepository)
                .toD(CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_URI)
                .log("Record sent to RabbitMQ: ${body}")
                .endChoice()
                .otherwise()
                .toD(CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_URI)
                .log("Record sent to RabbitMQ without idempotency check: ${body}")
                .end()
                .process(exchange -> {
                    // Handle publisher confirms asynchronously
                    Channel channel = exchange.getProperty("rabbitmqChannel", Channel.class);
                    if (channel != null) {
                        channel.addConfirmListener(new ConfirmListener() {
                            @Override
                            public void handleAck(long deliveryTag, boolean multiple) {
                                log.info("Message confirmed with deliveryTag: {}", deliveryTag);
                            }

                            @Override
                            public void handleNack(long deliveryTag, boolean multiple) {
                                log.error("Message NOT confirmed with deliveryTag: {}", deliveryTag);
                                // Implement retry logic or dead letter handling
                                handleNackedMessage(deliveryTag, deliveryTag,exchange.getProperty("localSessionId",String.class));
                            }
                        });
                    }
                });
    }

    private void configureConsumerRoute(MnoMapping airtelmoneyTanzania) {
        from(CamelConfiguration.RABBIT_CONSUMER_AIRTEL_MONEY_URI)
                .routeId("ussd-airtelmoney-deposits-consumer")
                .log("Received record from RabbitMQ: ${body}")
                .doTry()
                // Store original delivery info
                .process(exchange -> {
                    exchange.setProperty("originalChannel",
                            exchange.getIn().getHeader(SpringRabbitMQConstants.CHANNEL));
                    exchange.setProperty("originalDeliveryTag",
                            exchange.getIn().getHeader(SpringRabbitMQConstants.DELIVERY_TAG));
                })
                // First check with IdempotencyService for faster cache-based deduplication
                .process(this::processDeduplicationWithRedis)
                .filter(body().isNotNull())  // Only proceed if not a duplicate from idempotency check
                // Now check with DeduplicationService for database-level deduplication and mark as processing
                .bean(DeduplicationService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate from deduplication check
                .process(this::process)
                .choice()
                .when(header("mobileMoneyName").isEqualTo("AirtelMoney-Tanzania"))
                .log("Mobile Money Name Found: ${header.mobileMoneyName}")
                .toD("direct:airtelmoney-tanzania-payment-processor")
                .otherwise()
                .log("Mobile Money Name Not Found: ${header.mobileMoneyName}")
                .end()
                .end() // End of filter
                .end() // End of filter
                // Acknowledge on successful processing
                .bean(RabbitMqAckProcessor.class, "acknowledge")
                .endDoTry()
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    int redeliveryCount = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, 0, Integer.class);

                    // Reject with or without requeue based on retry count
                    boolean requeue = redeliveryCount < 3; // Only requeue if we haven't exceeded max retries
                    exchange.getContext().getRegistry()
                            .lookupByNameAndType("rabbitMqAckProcessor", RabbitMqAckProcessor.class)
                            .rejectWithRequeue(exchange, requeue);

                    // Update status if max retries reached
                    if (!requeue) {
                        PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                        if (PayBillPayment != null) {
                            transactionLogServiceImpl.updateToFailed(PayBillPayment);
                            idempotencyService.markRecordAsCompleted(PayBillPayment, dedupExpiryHours);
                        }

                        // Update session
                        String sessionId = exchange.getProperty("sessionId", String.class);
                        if (sessionId != null) {
                            updateSession(sessionId, "FAILED", "Payment processing failed");
                            exchange.setProperty("failedMessage", "Payment processing failed");
                            // Send callback to vendor
                            exchange.getContext().createProducerTemplate().send("direct:airtel-tanzania-record-failed-deposits", exchange);
                        }
                    }
                })
                .end();
    }

    private void configurePaymentProcessorRoute(MnoMapping airtelmoneyTanzania) {
        // Determine throttling settings from MNO config or default value
        int tps = airtelmoneyTanzania.getTps() > 0 ? airtelmoneyTanzania.getTps() : airtelTps;

        // Payment processing route
        from("direct:airtelmoney-tanzania-payment-processor")
                .threads().executorService(airtelVirtualThread)
                .throttle(tps).timePeriodMillis(1000) // Dynamic TPS based on MNO config
                .routeId("airtelmoney-tanzania-payment-processor")
                .log("Processing airtelmoney-Tanzania Payment: ${body}")
                .doTry()
                .process(exchange -> {
                    // Store original body for later restoration
                    exchange.setProperty("originalJsonBody", exchange.getIn().getBody());
                })
                .choice()
                // Case 1: Both commission and VAT enabled
                .when(simple("${exchangeProperty.vendorDto.hasCommission} == 'true' && ${exchangeProperty.vendorDto.hasVat} == 'true' && ${exchangeProperty.ussdStatus} == '0'"))
                .log("Processing both Commission and VAT")
                .process(this::processCommissionAndVatSync)
                // Case 2: Only commission enabled, no VAT
                .when(simple("${exchangeProperty.vendorDto.hasCommission} == 'true' && ${exchangeProperty.vendorDto.hasVat} != 'true' && ${exchangeProperty.ussdStatus} == '0'"))
                .log("Processing Commission only")
                .process(this::processCommissionSync)
                // Case 3: Only VAT enabled, no commission
                .when(simple("${exchangeProperty.vendorDto.hasCommission} != 'true' && ${exchangeProperty.vendorDto.hasVat} == 'true' && ${exchangeProperty.ussdStatus} == '0'"))
                .log("Processing VAT only")
                .process(this::processVatSync)
                // Case 4: Neither commission nor VAT enabled
                .otherwise()
                .log("No Commission or VAT processing required")
                .toD("direct:airtelmoney-tanzania-deposit-processor")
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



    private void configureDepositRoute(){
        from("direct:airtelmoney-tanzania-deposit-processor")
                .routeId("airtelmoney-tanzania-deposit-processor")
                .log("Processing airtelmoney-Tanzania Deposit: ${body}")
                .doTry()
                .process(exchange -> {
                    // Restore original body for deposit processing
                    Object originalBody = exchange.getProperty("originalJsonBody");
                    if (originalBody != null) {
                        exchange.getIn().setBody(originalBody);
                    }
                })
                .process(exchange -> {
                    log.info("Processing MSISDN: {}", exchange.getIn().getHeader("msisdn", String.class));

                    // Double-check idempotency before processing
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null && idempotencyService.isRecordProcessed(PayBillPayment)) {
                        log.info("Detected duplicate transaction at payment processor stage: ID {}, Reference {}",
                                PayBillPayment.getId(), PayBillPayment.getPaymentReference());
                        exchange.setProperty("skipProcessing", true);
                        return;
                    }

                    log.info("Is PayBillPayment null? {}", PayBillPayment == null);

                    if (PayBillPayment == null) {
                        throw new IllegalStateException("PayBillPayment must not be null at this stage");
                    }

                    // Initialize deposit with required parameters
                    try {
                        this.depositProcessor.initDeposit(PayBillPayment).join();
                        log.info("Deposit initialized successfully for MSISDN: {}", PayBillPayment.getMsisdn());
                    } catch (Exception e) {
                        log.error("Error initializing deposit for MSISDN: {}", PayBillPayment.getMsisdn(), e);
                        throw new RuntimeException("Deposit initialization failed", e);
                    }

                    // Build deposit request object
                    DepositRequest depositRequest = DepositRequest.builder()
                            .transactionNo(PayBillPayment.getReceiptNumber())
                            .reference(this.depositResources.extractOriginalReference(exchange).join())
                            .message(exchange.getIn().getHeader("message", String.class))
                            .status(PayBillPayment.getStatus().equalsIgnoreCase("0") ? "SUCCESS" : "FAILED")
                            .paymentSessionId(exchange.getProperty("sessionId", String.class))
                            .build();

                    exchange.setProperty("depositRequest", depositRequest);

                    // Convert deposit request to JSON
                    ObjectMapper mapper = new ObjectMapper();
                    String apiJsonRq = mapper.writeValueAsString(depositRequest);
                    log.info("API payload: {}", apiJsonRq);

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
                .setHeader("Idempotency-Key", simple("${header.PayBillPayment.id}-${header.PayBillPayment.reference}-${header.PayBillPayment.receiptNumber}-${date:now:yyyyMMddHHmmss}"))
                .log("AirtelMoney [Scoop] Deposit Request Body: ${body}")
                // Make HTTP call to deposit URL

                // Using wireTap for fire-and-forget async callback
                .wireTap("direct:airtel-money-tanzania-async-callback")
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
                    log.info("AirtelMoney [Scoop] Deposit Response (immediate): {}", responseJson);
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
                        this.depositProcessor.completeDeposit(depositResponse, exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class)).join();
                        log.info("Deposit completed successfully for MSISDN: {}", exchange.getIn().getHeader("msisdn", String.class));
                    } catch (Exception e) {
                        log.error("Error completing deposit for MSISDN: {}", exchange.getIn().getHeader("msisdn", String.class), e);
                        throw new RuntimeException("Deposit completion failed", e);
                    }
                })
                .choice()
                .when(simple("${exchangeProperty.ussdStatus} == '-1'"))
                .log("USSD status is -1, skipping update account balance")
                .toD("direct:airtelmoney-update-ussd-push-status")
                .when(simple("${exchangeProperty.ussdStatus} == '0'"))
                .log("USSD status is 0, updating account balance")
                .toD("direct:airtelmoney-update-account-balance")
                .otherwise()
                .log("USSD status is neither -1 nor 0: ${exchangeProperty.ussdStatus}")
                .end()
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

    // Fire-and-forget async callback using wireTap
    private void configureAsyncCallbackRoute() {
        from("direct:airtel-money-tanzania-async-callback")
                .routeId("airtel-money-tanzania-async-callback-processor")
                .threads().executorService(airtelVirtualThread)
                .log("Processing async callback: ${body}")
                .doTry()
                .toD("${exchangeProperty.callbackUrl}?bridgeEndpoint=true&httpMethod=POST&connectTimeout=5000&socketTimeout=10000")
                .log("Async callback successful: ${body}")
                .process(exchange -> {
                    // Optionally log successful callback
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null) {
                        log.info("Callback successful for transaction ID: {}, Reference: {}",
                                PayBillPayment.getId(), PayBillPayment.getPaymentReference());
                    }
                })
                .doCatch(org.apache.camel.http.base.HttpOperationFailedException.class)
                .log(LoggingLevel.WARN, "Async callback HTTP error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null) {
                        this.logCallbackFailure(PayBillPayment, "HTTP Error: " + e.getMessage()).join();
                    }
                })
                .doCatch(java.net.ConnectException.class, java.net.SocketTimeoutException.class)
                .log(LoggingLevel.WARN, "Async callback connection/timeout error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null) {
                        this.logCallbackFailure(PayBillPayment, "Connection/Timeout Error: " + e.getMessage()).join();
                    }
                })
                .doCatch(Exception.class)
                .log(LoggingLevel.WARN, "Async callback general error: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null) {
                        this.logCallbackFailure(PayBillPayment, "General Error: " + e.getMessage()).join();
                    }
                })
                .end();
    }

    private CompletableFuture<Void> logCallbackFailure(PayBillPayment PayBillPayment, String callbackError) {
        // Add callback failure to database

        FailedCallBack failedCallBack = FailedCallBack.builder()
                .msisdn(PayBillPayment.getMsisdn())
                .reference(PayBillPayment.getPaymentReference())
                .originalReference(PayBillPayment.getReceiptNumber())
                .errorMessage(callbackError)
                .payBillPayment(PayBillPayment)
                .build();

        // Save to database asynchronously
        return CompletableFuture.runAsync(() -> failedCallBackService.createFailedCallBack(failedCallBack), airtelVirtualThread);

    }

    private void recordFailedDeposits() {
        // Vendor Callback route
        from("direct:airtel-tanzania-record-failed-deposits")
                .routeId("airtel-tanzania-record-failed-deposits-route")
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

    private void processVendorCallback(Exchange exchange, String sessionId) {
        AtomicReference<PayBillPayment> PayBillPaymentReference = new AtomicReference<>();
        this.payBillPaymentService.findByPaymentSessionId(sessionId).ifPresent(PayBillPaymentReference::set);
        // Build deposit request object
        FailedDepositRequest depositRequest = FailedDepositRequest.builder()
                .transactionNo(PayBillPaymentReference.get().getReceiptNumber())
                .reference(PayBillPaymentReference.get().getReceiptNumber())
                .message(exchange.getProperty("failedMessage", String.class))
                .status("FAILED")
                .channel("PUSH_USSD")
                .paymentSessionId(exchange.getProperty("sessionId", String.class))
                .callbackUrl(PayBillPaymentReference.get().getVendorDetails().getCallbackUrl())
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

    private void processCommissionAndVatSync(Exchange exchange) {
        VendorDto vendorDto = exchange.getProperty("vendorDto", VendorDto.class);
        String sessionId = exchange.getProperty("sessionId", String.class);
        BigDecimal amount = BigDecimal.valueOf(exchange.getIn().getHeader("amount", Float.class));
        String msisdn = exchange.getIn().getHeader("msisdn", String.class);
        String collectionType = exchange.getIn().getHeader("collectionType", String.class);
        String paymentReference = exchange.getProperty("paymentReference",String.class);
        PayBillPayment payBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
        log.debug(">>>>>>>>>>>>>>>>>>>> Processing commission and VAT for sessionId: " + sessionId);
        try {
            // Process commission synchronously
            this.depositResources.processCommission(vendorDto, sessionId, amount, msisdn, collectionType,paymentReference,null, payBillPayment, null).join();
            log.info("Commission processed successfully for sessionId: {}", sessionId);

            // Process VAT synchronously
            Exchange vatExchange = exchange.getContext().createProducerTemplate().send("direct:process-vat", exchange);

            // Ensure the exchange continues with the original state
            exchange.getIn().setBody(vatExchange.getIn().getBody());
            //exchange.setProperty(vatExchange.getProperties());
            log.info("VAT processed successfully for sessionId: {}", sessionId);

            // Continue processing deposits
            exchange.getContext().createProducerTemplate().send("direct:airtelmoney-tanzania-deposit-processor", exchange);

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
            exchange.getContext().createProducerTemplate().send("direct:airtelmoney-tanzania-deposit-processor", exchange);
        } catch (Exception e) {
            log.error("Error processing VAT for sessionId: {}", exchange.getProperty("sessionId", String.class), e);
            throw new RuntimeException("VAT processing failed", e);
        }
    }

    private void processCommissionSync(Exchange exchange) {
        VendorDto vendorDto = exchange.getProperty("vendorDto", VendorDto.class);
        String sessionId = exchange.getProperty("sessionId", String.class);
        BigDecimal amount = BigDecimal.valueOf(exchange.getIn().getHeader("amount", Float.class));
        String msisdn = exchange.getIn().getHeader("msisdn", String.class);
        String collectionType = exchange.getIn().getHeader("collectionType", String.class);
        String paymentReference = exchange.getProperty("paymentReference",String.class);
        PayBillPayment payBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);

        try {
            this.depositResources.processCommission(vendorDto, sessionId, amount, msisdn, collectionType, paymentReference,null, payBillPayment,null).join();
            log.info("Commission processed successfully for sessionId: {}", sessionId);

            // Continue processing deposits
            exchange.getContext().createProducerTemplate().send("direct:airtelmoney-tanzania-deposit-processor", exchange);
        } catch (Exception e) {
            log.error("Error processing commission for sessionId: {}", sessionId, e);
            throw new RuntimeException("Commission processing failed", e);
        }
    }


    private void configureStatusUpdateRoutes() {
        // Update account balance route
        from("direct:airtelmoney-update-account-balance")
                .threads().executorService(airtelVirtualThread)
                .routeId("airtelmoney-update-account-balance")
                .log("Processing update-account-balance: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    this.depositProcessor.updateDepositBalance(exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class))
                            .thenAcceptAsync(result -> {
                                if (result) {
                                    exchange.getContext().createProducerTemplate()
                                            .send("direct:airtelmoney-update-ussd-push-status", exchange);
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
        from("direct:airtelmoney-update-ussd-push-status")
                .routeId("airtelmoney-update-ussd-push-status")
                .log("Processing update-ussd-push-status: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    this.depositProcessor.updateUssdPushStatusAsync(exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class))
                            .thenAcceptAsync(success -> {
                                if (success) {
                                    log.info("[Scoop] Push Ussd Status updated successfully");

                                    exchange.getContext().createProducerTemplate()
                                            .send("direct:airtelmoney-update-deposit-txn-status", exchange);
                                }
                            });
                })
                //.toD("direct:airtelmoney-update-deposit-txn-status")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
                .process(exchange -> {
                    Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.error("Processing error: ", e);
                    throw e;
                })
                .end();

        // Update deposit transaction status route
        from("direct:airtelmoney-update-deposit-txn-status")
                .routeId("airtelmoney-update-deposit-txn-status")
                .log("Processing update-deposit-txn-status: ${body}")
                .doTry()
                .process(exchange -> {
                    //DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    this.depositProcessor.updateDepositTransactionStatusAsync(exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class))
                            .thenAcceptAsync(success -> {
                                if (success) {
                                    log.info("[Scoop] Deposit transaction status updated successfully");
                                }
                            });

                    // Mark record as completed in idempotency service
                    PayBillPayment PayBillPayment = exchange.getIn().getHeader("PayBillPayment", PayBillPayment.class);
                    if (PayBillPayment != null) {
                        // Use the full expiry time when transaction completes successfully
                        idempotencyService.markRecordAsCompleted(PayBillPayment, dedupExpiryHours);
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
        PayBillPaymentDto PayBillPaymentDto = null;
        try {
            PayBillPaymentDto = mapper.readValue(exchange.getIn().getBody(String.class), PayBillPaymentDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        AtomicLong accountId = new AtomicLong(0L);
        AtomicReference<PayBillPayment> PayBillPayment = new AtomicReference<>();

        this.payBillPaymentService.findById(PayBillPaymentDto.getId()).ifPresent(PayBillPayment1 -> {
            accountId.set(Long.parseLong(PayBillPayment1.getAccountId()));
            PayBillPayment.set(PayBillPayment1);

            // Record this processing in idempotency service as early as possible
            idempotencyService.markRecordAsProcessing(PayBillPayment1, processingTimeoutMinutes);
        });

        String mobileMoneyName = this.mnoService.searchMno(PayBillPaymentDto.getMsisdn());

        exchange.getIn().setHeader("mobileMoneyName", mobileMoneyName);
        exchange.getIn().setHeader("message", "Payment completed");
        exchange.getIn().setHeader("msisdn", PayBillPaymentDto.getMsisdn());
        exchange.getIn().setHeader("PayBillPaymentId", PayBillPaymentDto.getId());
        exchange.getIn().setHeader("mainAccountId", accountId.get());
        exchange.setProperty("sessionId", PayBillPayment.get().getSessionId());
        exchange.setProperty("callbackUrl", PayBillPayment.get().getVendorDetails().getCallbackUrl());
        exchange.getIn().setHeader("amount", PayBillPaymentDto.getAmount());
        exchange.getIn().setHeader("collectionType", PayBillPaymentDto.getCollectionType());
        exchange.setProperty("paymentReference", PayBillPaymentDto.getPaymentReference());
        exchange.getIn().setHeader("PayBillPayment", PayBillPayment.get());
        exchange.setProperty("vendorDto", PayBillPaymentDto.getVendorDto());
        exchange.setProperty("ussdStatus", PayBillPaymentDto.getStatus());
    }

    private void processDeduplicationWithRedis(Exchange exchange) {
        String messageBody = exchange.getIn().getBody(String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            PayBillPaymentDto dto = mapper.readValue(messageBody, PayBillPaymentDto.class);

            boolean isDuplicate = false;
            // Check idempotency service first (Redis-based fast check)
            if (dto != null && dto.getId() != null) {
                // Option 1: Check by ID
                isDuplicate = idempotencyService.isRecordProcessed(dto.getId().toString());

                // Option 2: If not found by ID, reconstruct a PayBillPayment for composite key check
                if (!isDuplicate) {
                    PayBillPayment tempRecord = new PayBillPayment();
                    tempRecord.setId(dto.getId());
                    tempRecord.setPaymentReference(dto.getPaymentReference());
                    tempRecord.setMsisdn(dto.getMsisdn());
                    tempRecord.setOperator(dto.getOperator());
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

    // Immediately retry the message after a nack (for transient errors)
    private void handleNackedMessage(Object message, long deliveryTag, String sessionId) {
        int maxRetries = 3;
        int retryDelayMs = 1000; // 1 second delay

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Re-publish the message
                rabbitTemplate.convertAndSend(
                        CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_URI,
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
