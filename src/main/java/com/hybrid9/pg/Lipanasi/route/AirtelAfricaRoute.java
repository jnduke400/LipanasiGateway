package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.AirtelMoneyTokenResponse;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.ResponseDto;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.AirtelMoneyConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.route.handler.exceptions.ScoopExceptionHandlers;
import com.hybrid9.pg.Lipanasi.route.handler.mixbyyas.BillerPaymentRequestBuilder;
import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.AirtelCircuitBreakerProcessor;
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
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.springrabbit.SpringRabbitMQConstants;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

//@Component
public class AirtelAfricaRoute extends RouteBuilder {

    @Autowired
    @Qualifier("airtelMoneyThreadPool")
    private ThreadPoolTaskExecutor airtelMoneyThreadPool;

    @Autowired
    @Qualifier("airtelHttpClient")
    private HttpClient airtelHttpClient;

    @Autowired
    @Qualifier("airtelCircuitBreaker")
    private CircuitBreaker airtelCircuitBreaker;

    @Autowired
    @Qualifier("airtelMoneyInitRabbitTemplate")
    private RabbitTemplate rabbitTemplate;
    private final ServiceNameComponent saviceNameComponent;
    private final CircuitBreakerService circuitBreakerService;
    private final BillerPaymentRequestBuilder requestBuilder;
    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;
    private final SessionManagementService sessionManagementService;
    private final ScoopExceptionHandlers scoopExceptionHandlers;

    public AirtelAfricaRoute(BillerPaymentRequestBuilder requestBuilder, CashInLogService cashInLogService, MnoServiceImpl mnoService, DepositService depositService, MainAccountService mainAccountService
            , VendorService vendorService, PushUssdService pushUssdService, CircuitBreakerService circuitBreakerService, ServiceNameComponent saviceNameComponent,
                             SessionManagementService sessionManagementService, ScoopExceptionHandlers scoopExceptionHandlers) {
        this.requestBuilder = requestBuilder;
        this.cashInLogService = cashInLogService;
        this.mnoService = mnoService;
        this.depositService = depositService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.pushUssdService = pushUssdService;
        this.circuitBreakerService = circuitBreakerService;
        this.saviceNameComponent = saviceNameComponent;
        this.sessionManagementService = sessionManagementService;
        this.scoopExceptionHandlers = scoopExceptionHandlers;
    }

    private static final Logger logger = LoggerFactory.getLogger(AirtelAfricaRoute.class);

    @Override
    public void configure() throws Exception {
        // Register HTTP clients
        getContext().getRegistry().bind("airtelHttpClient", airtelHttpClient);

        saviceNameComponent.setServiceName(ServiceName.AIRTEL_PAYMENT);

        // Create circuit breaker processor
        AirtelCircuitBreakerProcessor airtelCircuitProcessor = new AirtelCircuitBreakerProcessor(
                airtelCircuitBreaker,
                cashInLogService,
                depositService,
                saviceNameComponent
        );

        //Register Processor
        getContext().getRegistry().bind("airtelCircuitProcessor", airtelCircuitProcessor);

        // Error Handler with conditional retry policy
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                // Add retry condition - only retry for retryable exceptions
                .retryWhile(this::shouldRetryCheck)
                .onRedelivery(exchange -> {
                    // Update retry count and status in cash_in_logs for retryable exceptions only
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        int retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);

                        cashInLog.setRetryCount(retryCount);
                        if (retryCount < 3) {
                            cashInLog.setStatus(RequestStatus.MARKED_FOR_RETRY);
                            //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
                        } else {
                            cashInLog.setStatus(RequestStatus.FAILED);
                            //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
                            // Update deposit transaction when max retries reached
                            String transactionId = exchange.getProperty("transactionId", String.class);
                            depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                                deposit.setRequestStatus(RequestStatus.FAILED);
                                deposit.setErrorMessage("Maximum retry attempts reached - Transaction failed");
                                depositService.recordDeposit(deposit);
                            });

                            // Update session
                            String sessionId = exchange.getProperty("sessionId", String.class);
                            if (sessionId != null) {
                                updateSession(sessionId, "FAILED", "Payment processing failed");
                                exchange.setProperty("failedMessage", "Payment initiation failed");
                                // Send callback to vendor
                                exchange.getContext().createProducerTemplate().send("direct:airtel-tanzania-init-vendor-callback", exchange);
                            }
                        }
                        cashInLogService.recordLog(cashInLog);
                    }
                }));

        // Rest of your existing route configuration...
        MnoMapping airtelmoneyTanzania = this.mnoService.findMappingByMno("AirtelMoney-Tanzania");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(airtelmoneyTanzania.getMno());

        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);

        from("quartz://depositInitiation/airtelMoney?cron=0/1+*+*+*+*+?&stateful=false") // Trigger every 30 seconds
                .routeId("init-airtel-money-deposits-producer")
                .threads().executorService(airtelMoneyThreadPool.getThreadPoolExecutor())
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
                .toD(CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_INIT_URI)
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
                                logger.error("Message NOT confirmed with deliveryTag: {}", deliveryTag);

                                // Retry logic or dead letter handling
                                handleNackedMessage(originalMessage, deliveryTag,depositDto.getSessionId());
                            }
                        });
                    }
                })
                .log("Record sent to RabbitMQ: ${body}")
                .end();

        // Token Management Route
        from("direct:getAirtelMoneyToken")
                .id("getTokenRoute")
                // Store original body in a property
                .setProperty("originalBody", simple("${body}"))
                /*.setBody(constant("""
                        {
                            "client_id": "${exchangeProperty.networkConfig.clientId}",
                            "client_secret": "${exchangeProperty.networkConfig.clientSecret}",
                            "grant_type": "client_credentials"
                        }
                        """))*/
                .process(exchange -> {
                    // Get the network config from exchange property
                    AirtelMoneyConfig networkConfig = exchange.getProperty("networkConfig", AirtelMoneyConfig.class);

                    if (networkConfig == null) {
                        throw new RuntimeException("Network configuration not found in exchange properties");
                    }

                    // Construct the token request body with actual values
                    String tokenRequestBody = String.format("""
                            {
                                "client_id": "%s",
                                "client_secret": "%s",
                                "grant_type": "client_credentials"
                            }
                            """, networkConfig.getClientId(), networkConfig.getClientSecret());

                    exchange.getIn().setBody(tokenRequestBody);

                    logger.debug("Token request body prepared for client_id: {}", networkConfig.getClientId());
                })
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("*/*"))
                .log("Airtel Money Token Request Body: ${body}")
                .setHeader("CamelHttpClient", constant(airtelHttpClient))
                .toD("${exchangeProperty.networkConfig.tokenUrl}?bridgeEndpoint=true&httpMethod=POST")
                .convertBodyTo(String.class)
                .log("Airtel Money Token Response: ${body}")
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();
                    AirtelMoneyTokenResponse tokenResponse = mapper.readValue(exchange.getIn().getBody(String.class), AirtelMoneyTokenResponse.class);
                    exchange.getMessage().setHeader("auth_token", tokenResponse.getAccess_token());
                    // Restore original body
                    exchange.getMessage().setBody(exchange.getProperty("originalBody"));
                })
                .setProperty("auth_token", simple("${header.auth_token}"));

        // Main Payment Route
        from(CamelConfiguration.RABBIT_CONSUMER_AIRTEL_MONEY_INIT_URI)
                .id("init-airtel-money-deposits-consumer")
                .bean(InitDepositDeduplicationService.class, "checkAndMarkInitDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(exchange -> {
                    //set header
                    ObjectMapper mapper = new ObjectMapper();
                    DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);
                    exchange.setProperty("msisdn", depositDto.getMsisdn());
                    exchange.setProperty("amount", String.valueOf(depositDto.getAmount()).replaceAll("\\.\\d+", ""));
                    exchange.setProperty("reference", depositDto.getPaymentReference());
                    exchange.setProperty("operator", depositDto.getOperator());
                    exchange.setProperty("transactionId", depositDto.getTransactionId());
                    exchange.setProperty("sessionId", depositDto.getSessionId());
                    exchange.setProperty("networkConfig", (AirtelMoneyConfig) this.sessionManagementService.getSession(depositDto.getSessionId()).orElseThrow(() ->
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
                .process(exchange -> {
                    // Get the network config from exchange property
                    AirtelMoneyConfig networkConfig = exchange.getProperty("networkConfig", AirtelMoneyConfig.class);

                    if (networkConfig == null) {
                        throw new RuntimeException("Network configuration not found in exchange properties");
                    }
                    // Extract payment details from the request
                    String msisdn = exchange.getProperty("msisdn", String.class);
                    String amount = exchange.getProperty("amount", String.class);
                    String transactionId = exchange.getProperty("transactionId", String.class);
                    String reference = exchange.getProperty("reference", String.class);

                    // Construct the payment request body with actual values
                    String paymentRequestBody = String.format("""
                            {
                                "reference": "%s",
                                "subscriber": {
                                    "country": "%s",
                                    "currency": "%s",
                                    "msisdn": "%s"
                                },
                                "transaction": {
                                    "amount": %s,
                                    "country": "%s",
                                    "currency": "%s",
                                    "id": "%s"
                                }
                            }
                            """, reference, networkConfig.getCountry(), networkConfig.getCurrency(), msisdn.substring(3), amount, networkConfig.getCountry(), networkConfig.getCurrency(), transactionId);

                    exchange.getIn().setBody(paymentRequestBody);

                    logger.debug("Payment request body prepared for reference: {}", exchange.getIn().getHeader("reference", String.class));
                })
                .process("airtelCircuitProcessor")
                .doTry()
                // First we get the token
                .toD("direct:getAirtelMoneyToken")
                // Then make the payment request
                // Remove all existing headers except the ones we want to keep
                .removeHeaders("*")
                // Then we set our specific headers
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("*/*"))
                .setHeader("X-Country", constant("TZ"))
                .setHeader("X-Currency", constant("TZS"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.auth_token}"))
                //log request headers
                .log("Airtel Money Request Headers: ${headers}")
                .log("Airtel Money Payment Body: ${body}")
                .setHeader("CamelHttpClient", constant(airtelHttpClient))
                .toD("${exchangeProperty.networkConfig.apiUrl}?bridgeEndpoint=true&httpMethod=POST")
                // Handle the response
                .log("Airtel Money Response: ${body}")
                .process(exchange -> {
                    // Record successful call to circuit breaker
                    circuitBreakerService.recordSuccess("airtelPaymentService");
                })
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .toD("direct:handleAirtelMoneySuccessResponse")
                .otherwise()
                .toD("direct:handleAirtelMoneyFailureResponse")
                .end()
                .endDoTry()
                .doCatch(Exception.class)
                .process(exchange -> {
                    //logger.error(">>>>>>>>>>>>>>>>>>>>[message] Airtel payment failed: {}", exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage());
                    //logger.error(">>>>>>>>>>>>>>>>>>>>[exception.message] Airtel payment failed: {}", exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class));

                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
                    String errorPrefix = "Airtel payment failed: ";

                    // Only record circuit breaker failure for service-related issues
                    if (!(exception instanceof AirtelCircuitBreakerProcessor.CircuitBreakerOpenException)
                            && this.scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
                        circuitBreakerService.recordFailure("airtelPaymentService", exception);
                    }

                    //logger.error(">>>>>>>>>>>>>>>>>>>>[Error Message] Airtel payment failed: {}", exception.getMessage());

                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        if (cashInLog != null) {
                            cashInLog.setStatus(status);
                            cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
                            //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
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
                                case NETWORK_CONNECTION_ISSUE -> "Could not connect to Airtel servers";
                                case TIMEOUT -> "Airtel service did not respond in time";
                                case SERVICE_UNAVAILABLE -> "Airtel service is currently unavailable";
                                case INVALID_CREDENTIALS -> "Authentication failed with Airtel service";
                                case INVALID_REQUEST -> "Invalid request format or parameters";
                                case BUSINESS_RULE_VIOLATION -> "Transaction violates business rules";
                                default -> "Payment processing failed: " + exception.getMessage();
                            };

                            deposit.setErrorMessage(errorMessage);
                            depositService.recordDeposit(deposit);
                        });
                    }
                })
                .log(LoggingLevel.ERROR, "Exception encountered: ${exception.message}")
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

        // Success Response Handler
        from("direct:handleAirtelMoneySuccessResponse")
                .id("AirtelMoney-successResponseRoute")
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ResponseDto response = mapper.readValue(exchange.getMessage().getBody(String.class), ResponseDto.class);
                    exchange.getMessage().setHeader("responseStatus", response.getStatus().getSuccess());

                    // Store transaction details for callback
                    this.depositService.findByTransactionId(exchange.getProperty("transactionId", String.class)).ifPresent(deposit -> {
                        PushUssd pushUssd = PushUssd.builder()
                                .status(response.getStatus().getSuccess() ? "0" : "-1")
                                .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                .message(response.getStatus().getMessage())
                                .reference(exchange.getProperty("reference", String.class))
                                .currency(deposit.getCurrency())
                                .operator(this.mnoService.searchMno(deposit.getMsisdn()))
                                .sessionId(deposit.getSessionId())
                                .accountId(String.valueOf(mainAccountService.findByVendorDetails(deposit.getVendorDetails()).getId()))
                                .vendorDetails(deposit.getVendorDetails())
                                .msisdn(deposit.getMsisdn())
                                .build();
                        this.pushUssdService.update(pushUssd);
                        exchange.setProperty("transactionId", exchange.getProperty("transactionId", String.class));

                        // Update session
                        String sessionId = exchange.getProperty("sessionId", String.class);
                        if (sessionId != null && !response.getStatus().getSuccess()) {
                            updateSession(sessionId, "FAILED", "Payment processing failed");
                            exchange.setProperty("failedMessage", "Payment initiation failed");
                            // Send callback to vendor
                            exchange.getContext().createProducerTemplate().send("direct:airtel-tanzania-init-vendor-callback", exchange);
                        }
                    });
                })
                .log("Response Status: ${header.responseStatus}")
                .end();

        // Error Response Handler for Network Connection Issues
        from("direct:handleAirtelMoneyFailureResponse")
                .id("airtelMoney-errorResponseRoute")
                .process(exchange -> {
                    // Get HTTP response code to determine error type
                    Integer responseCode = exchange.getIn().getHeader("CamelHttpResponseCode", Integer.class);
                    RequestStatus status = this.scoopExceptionHandlers.classifyHttpResponseCode(responseCode);

                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        if (cashInLog != null) {
                            cashInLog.setStatus(status);
                            cashInLog.setErrorMessage(getErrorMessageForStatus(status, responseCode));
                            //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
                            cashInLogService.recordLog(cashInLog);
                        }
                    }

                    // Update deposit transaction
                    String transactionId = exchange.getProperty("transactionId", String.class);
                    depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                        deposit.setRequestStatus(status);
                        deposit.setErrorMessage(getErrorMessageForStatus(status, responseCode));
                        depositService.recordDeposit(deposit);
                    });
                })
                .log(LoggingLevel.ERROR, "HTTP error response encountered: ${header.CamelHttpResponseCode}");

        // Vendor Callback route
        from("direct:airtel-tanzania-init-vendor-callback")
                .routeId("airtel-tanzania-init-vendor-callback-route")
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
                        CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_INIT_URI,
                        message
                );
                logger.info("Retry attempt {} successful for deliveryTag: {}", attempt, deliveryTag);
                return;
            } catch (Exception e) {
                logger.warn("Retry attempt {} failed for deliveryTag: {}", attempt, deliveryTag, e);
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

    private <T> T shouldRetryCheck(Exchange exchange, Class<T> tClass) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean shouldRetry = this.scoopExceptionHandlers.isRetryableException(exception);

        if (!shouldRetry) {
            log.info("Exception {} is non-retryable, skipping retry",
                    exception.getClass().getSimpleName());
            updateStatusForNonRetryableException(exchange, exception);
        }

        // Since retryWhile expects a Boolean, we can safely cast
        if (tClass == Boolean.class) {
            return tClass.cast(shouldRetry);
        }

        throw new IllegalArgumentException("Unsupported type: " + tClass);
    }


    // Helper method to update status for non-retryable exceptions immediately
    private void updateStatusForNonRetryableException(Exchange exchange, Exception exception) {
        RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
        String errorPrefix = "Airtel payment failed (non-retryable): ";

        // Update cash_in_logs
        String cashInLogId = exchange.getProperty("cashInLogId", String.class);
        if (cashInLogId != null) {
            CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
            if (cashInLog != null) {
                cashInLog.setStatus(status);
                cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
                //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
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
            case NETWORK_CONNECTION_ISSUE -> "Could not connect to Airtel servers";
            case TIMEOUT -> "Airtel service did not respond in time";
            case SERVICE_UNAVAILABLE -> responseCode != null && responseCode == 429
                    ? "Too many requests - please try again later"
                    : "Airtel service is currently unavailable";
            case INVALID_CREDENTIALS -> "Authentication failed with Airtel service";
            case INVALID_REQUEST -> "Invalid request format or parameters";
            case BUSINESS_RULE_VIOLATION -> "Transaction violates business rules";
            case RESOURCE_NOT_FOUND -> "Requested resource not found";
            default -> "Payment processing failed" + (responseCode != null ? " (HTTP " + responseCode + ")" : "");
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