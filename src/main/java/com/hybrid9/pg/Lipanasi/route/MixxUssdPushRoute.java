package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.deposit.FailedDepositRequest;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.BillerPaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.BillerPaymentResponse;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.TokenResponse;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MixxByYasConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.route.handler.exceptions.ScoopExceptionHandlers;
import com.hybrid9.pg.Lipanasi.route.handler.mixbyyas.BillerPaymentRequestBuilder;
import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.MixxCircuitBreakerProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.mixbyyas.PaymentRequestProcessor;
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
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.client.HttpClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

//@Component
public class MixxUssdPushRoute extends RouteBuilder {

    private static final int MAX_RETRIES = 3;

    @Autowired
    @Qualifier("mixxThreadPool")
    private ThreadPoolTaskExecutor mixxThreadPool;

    @Autowired
    @Qualifier("mixxHttpClient")
    private HttpClient mixxHttpClient;

    @Autowired
    @Qualifier("mixxCircuitBreaker")
    private CircuitBreaker mixxCircuitBreaker;
    @Autowired
    @Qualifier("tigopesaInitsRabbitTemplate")
    private RabbitTemplate rabbitTemplate;
    private final CircuitBreakerService circuitBreakerService;
    private final BillerPaymentRequestBuilder requestBuilder;
    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;
    private final ServiceNameComponent saviceNameComponent;
    private final SessionManagementService sessionManagementService;
    private final ScoopExceptionHandlers scoopExceptionHandlers;

    public MixxUssdPushRoute(BillerPaymentRequestBuilder requestBuilder, CashInLogService cashInLogService,
                             MnoServiceImpl mnoService, DepositService depositService,
                             MainAccountService mainAccountService, VendorService vendorService,
                             PushUssdService pushUssdService, CircuitBreakerService circuitBreakerService,
                             ServiceNameComponent saviceNameComponent, SessionManagementService sessionManagementService,
                             ScoopExceptionHandlers scoopExceptionHandlers) {
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

    @Override
    public void configure() throws Exception {
        // Register HTTP client
        getContext().getRegistry().bind("mixxHttpClient", mixxHttpClient);
        //set Service Name
        saviceNameComponent.setServiceName(ServiceName.MIXX_PAYMENT);
        // Create circuit breaker processor
        MixxCircuitBreakerProcessor mixxCircuitProcessor = new MixxCircuitBreakerProcessor(
                mixxCircuitBreaker,
                cashInLogService,
                depositService,
                saviceNameComponent
        );

        // Register processor
        getContext().getRegistry().bind("mixxCircuitProcessor", mixxCircuitProcessor);

        onException(BillerPaymentRequestBuilder.RequestCreationException.class)
                .handled(true)
                .process(exchange -> {
                    BillerPaymentResponse error = new BillerPaymentResponse();
                    error.setResponseCode("BILLER-18-3023-E");
                    error.setResponseStatus(false);
                    error.setResponseDescription("Failed to create biller payment request");
                    exchange.getMessage().setBody(error);

                    // Update session
                    String sessionId = exchange.getProperty("sessionId", String.class);
                    if (sessionId != null) {
                        updateSession(sessionId, "FAILED", "Payment processing failed");
                        exchange.setProperty("failedMessage", "Payment initiation failed");
                        // Send callback to vendor
                        exchange.getContext().createProducerTemplate().send("direct:mixx-tanzania-init-vendor-callback", exchange);
                    }
                })
                .log(LoggingLevel.ERROR, "Request creation failed: ${exception.message}");

        // Error Handler with retry policy
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(MAX_RETRIES)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retryWhile(this::shouldRetryCheck)
                .onRedelivery(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
                    String errorPrefix = "Mixx payment failed: ";

                    // ✅ Get correct retry count from Camel's redelivery counter
                    int retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);

                    log.info("++++++++++++++++++Retry Count: {} (Max: {})", retryCount, MAX_RETRIES);

                    // Only record circuit breaker failure for service-related issues
                    if (!(exception instanceof MixxCircuitBreakerProcessor.CircuitBreakerOpenException)
                            && this.scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
                        circuitBreakerService.recordFailure("mixxPaymentService", exception);
                    }

                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        if (cashInLog != null) {
                            // ✅ Always update retry count and status appropriately
                            cashInLog.setRetryCount(retryCount);

                            if (retryCount >= MAX_RETRIES) {
                                // Final failure - no more retries
                                cashInLog.setStatus(status);
                                cashInLog.setErrorMessage(errorPrefix + exception.getMessage());
                                //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));

                                // Update deposit transaction on final failure
                                updateDepositOnFinalFailure(exchange, status, exception);

                                // Update session
                                String sessionId = exchange.getProperty("sessionId", String.class);
                                if (sessionId != null) {
                                    updateSession(sessionId, "FAILED", "Payment processing failed");
                                    exchange.setProperty("failedMessage", "Payment initiation failed");
                                    // Send callback to vendor
                                    exchange.getContext().createProducerTemplate().send("direct:mixx-tanzania-init-vendor-callback", exchange);
                                }
                            } else {
                                // Still retrying
                                cashInLog.setStatus(RequestStatus.MARKED_FOR_RETRY);
                                cashInLog.setErrorMessage(errorPrefix + exception.getMessage() + " (Retry " + retryCount + "/" + MAX_RETRIES + ")");
                                //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
                            }

                            cashInLogService.recordLog(cashInLog);
                        }
                    }
                }));

        // Dead Letter Channel for failed messages after retries
        deadLetterChannel("direct:failedTransactions")
                .useOriginalMessage()
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        MnoMapping mixxbyyasTanzania = this.mnoService.findMappingByMno("Mixx_by_yas-Tanzania");
        MnoMapping zpesaTanzania = this.mnoService.findMappingByMno("ZPesa-Tanzania");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(mixxbyyasTanzania.getMno());
        mnoList.add(zpesaTanzania.getMno());


        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);

        from("quartz://depositInitiation/mixxbyyas?cron=0/1+*+*+*+*+?&stateful=false") // Trigger every 1 second
                .routeId("init-mixxbyyas-deposits-producer")
                .threads().executorService(mixxThreadPool.getThreadPoolExecutor())
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
                .toD(CamelConfiguration.RABBIT_PRODUCER_TIGOPESA_INITS_URI)
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

        // Route for token generation
        from("direct:getToken")
                .id("mixxbyyas-token-route")
                .doTry()
                .process(exchange -> {
                    // Get the network config from exchange property
                    MixxByYasConfig networkConfig = exchange.getProperty("networkConfig", MixxByYasConfig.class);

                    if (networkConfig == null) {
                        throw new RuntimeException("Network configuration not found in exchange properties");
                    }

                    // Prepare form parameters
                    String formData = String.format("username=%s&password=%s&grant_type=password",
                            URLEncoder.encode(networkConfig.getUsername(), StandardCharsets.UTF_8),
                            URLEncoder.encode(networkConfig.getPassword(), StandardCharsets.UTF_8));

                    // Set the body and headers
                    exchange.getMessage().setBody(formData);

                    log.info("Token Request Body: " + exchange.getMessage().getBody());

                    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
                    exchange.getMessage().setHeader(Exchange.HTTP_METHOD, "POST");
                })
                .setHeader("CamelHttpClient", constant(mixxHttpClient))
                //TODO: Uncomment this line when the token API is fixed
                /*.toD("${exchangeProperty.networkConfig.tokenUrl}?bridgeEndpoint=true&httpMethod=POST" +
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false" +
                        "&sslContextParameters=#noopSslContext")*/
                .toD("${exchangeProperty.networkConfig.tokenUrl}?bridgeEndpoint=true&httpMethod=POST" +
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false")
                .unmarshal().json(JsonLibrary.Jackson, TokenResponse.class)
                .process(exchange -> {
                    TokenResponse response = exchange.getMessage().getBody(TokenResponse.class);
                    exchange.setProperty("access_token", response.getAccess_token());
                })
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, "Token generation failed: ${exception.message}")
                .process(exchange -> {
                    // Handle token error
                    throw new RuntimeException("Failed to obtain authentication token");
                })
                .end();


        // Main USSD Push Route
        from(CamelConfiguration.RABBIT_CONSUMER_TIGOPESA_INITS_URI)
                .id("init-mixxbyyas-deposits-consumer")
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
                    exchange.setProperty("sessionId", depositDto.getSessionId()); // transactionId is the mixxbyyas transactionId
                    exchange.setProperty("transactionId", depositDto.getTransactionId());
                    exchange.setProperty("networkConfig", (MixxByYasConfig) this.sessionManagementService.getSession(depositDto.getSessionId()).orElseThrow(() ->
                            new RuntimeException("Session not found for ID: " + depositDto.getSessionId())).getNetworkConfig());
                    exchange.setProperty("billerMsisdn", ((MixxByYasConfig) this.sessionManagementService.getSession(depositDto.getSessionId()).orElseThrow(() ->
                            new RuntimeException("Session not found for ID: " + depositDto.getSessionId())).getNetworkConfig()).getBillerMsisdn());
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
                .process("mixxCircuitProcessor")
                .toD("direct:getToken")
                .process(exchange -> new PaymentRequestProcessor().process(exchange))
                .process(exchange -> {
                    // Get the network config from exchange property
                    MixxByYasConfig networkConfig = exchange.getProperty("networkConfig", MixxByYasConfig.class);

                    if (networkConfig == null) {
                        throw new RuntimeException("Network configuration not found in exchange properties");
                    }
                    exchange.setProperty("username", networkConfig.getUsername());
                    exchange.setProperty("password", networkConfig.getPassword());


                    try {
                        // Prepare biller payment request
                        BillerPaymentRequest request = requestBuilder.createBillerPaymentRequest(exchange);
                        ObjectMapper mapper = new ObjectMapper();
                        String json = mapper.writeValueAsString(request);
                        exchange.getMessage().setBody(json);
                    } catch (JsonProcessingException e) {
                        // Log the error
                        log.error("Failed to convert request to JSON", e);
                        // Update the cash in log
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
                        throw new RuntimeException("Failed to process request", e);
                    }
                })
                .removeHeaders("*")
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.access_token}"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Username", simple("${exchangeProperty.username}"))
                .setHeader("Password", simple("${exchangeProperty.password}"))
                //.setHeader(Exchange.HTTP_METHOD, constant("POST"))
                //log request headers
                .log("Mixx Request Headers: ${headers}")
                .log("Mixx Request Body: ${body}")
                .setHeader("CamelHttpClient", constant(mixxHttpClient))
                //TODO: Uncomment this line when the API is fixed
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
                .log("Mixx Response: ${body}")
                .process(exchange -> {
                    // Record successful call to circuit breaker
                    circuitBreakerService.recordSuccess("mixxPaymentService");
                })
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .toD("direct:handleSuccessResponse")
                .otherwise()
                .toD("direct:handleErrorResponse")
                .endChoice()
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

        // Error Response Handler for Network Connection Issues
        from("direct:handleErrorResponse")
                .id("mixxbyyas-errorResponseRoute")
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
                .log(LoggingLevel.ERROR, "Network connection issue encountered: ${exception.message}");


        // Error Response Handler for Network Connection Issues
        /*from("direct:handleErrorResponse")
                .id("errorResponseRoute")
                .process(exchange -> {
                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        cashInLog.setStatus(RequestStatus.NETWORK_CONNECTION_ISSUE);
                        cashInLogService.recordLog(cashInLog);
                    }

                    // Update deposit transaction
                    String transactionId = exchange.getIn().getHeader("transactionId", String.class);
                    depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                        deposit.setRequestStatus(RequestStatus.NETWORK_CONNECTION_ISSUE);
                        deposit.setErrorMessage("Network connection issue with mobile operator");
                        depositService.recordDeposit(deposit);
                    });
                })
                .log(LoggingLevel.ERROR, "Network connection issue encountered: ${exception.message}");*/

        // Success Response Handler
        from("direct:handleSuccessResponse")
                .id("successResponseRoute")
                .process(exchange -> {
                    String responseBody = exchange.getMessage().getBody(String.class);
                    if (responseBody != null && !responseBody.isEmpty()) {
                        ObjectMapper mapper = new ObjectMapper();
                        System.out.println("responseBody = " + responseBody);
                        BillerPaymentResponse response = mapper.readValue(responseBody, BillerPaymentResponse.class);

                        // Update the exchange properties
                        exchange.setProperty("responseStatus", response.isResponseStatus());
                        exchange.setProperty("responseCode", response.getResponseCode());
                        exchange.setProperty("responseReference", response.getReferenceID());

                        // Find and update the deposit
                        String transactionId = exchange.getProperty("transactionId", String.class);
                        if (transactionId != null) {
                            depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                                // Build and save PushUssd entry
                                PushUssd pushUssd = PushUssd.builder()
                                        .status(response.getResponseCode())
                                        .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                        .message(response.getResponseCode().equals("BILLER-18-0000-S")
                                                ? "Ussd Push Initiated Successfully"
                                                : "Ussd Push Initiated Failed")
                                        .reference(response.getReferenceID())
                                        .currency(deposit.getCurrency())
                                        .operator(mnoService.searchMno(deposit.getMsisdn()))
                                        .sessionId(deposit.getSessionId())
                                        .accountId(String.valueOf(mainAccountService.findByVendorDetails(deposit.getVendorDetails()).getId()))
                                        .vendorDetails(deposit.getVendorDetails())
                                        .msisdn(deposit.getMsisdn())
                                        .build();

                                // Update the status based on response
                               /* if (response.isResponseStatus() && "BILLER-18-0000-S".equals(response.getResponseCode())) {
                                    deposit.setRequestStatus(RequestStatus.SUCCESS);
                                } else {
                                    deposit.setRequestStatus(RequestStatus.FAILED);
                                    deposit.setErrorMessage(response.getResponseDescription());
                                }*/

                                // Save both entities
                                pushUssdService.update(pushUssd);
                                //depositService.recordDeposit(deposit);

                                // Update session
                                String sessionId = exchange.getProperty("sessionId", String.class);
                                if (sessionId != null && response.getResponseCode().equalsIgnoreCase("BILLER-18-3019-E")) {
                                    updateSession(sessionId, "FAILED", "Amount either exceeds the maximum amount or is less than the minimum amount");
                                    exchange.setProperty("failedMessage", "Payment initiation failed");
                                    // Send callback to vendor
                                    exchange.getContext().createProducerTemplate().send("direct:mixx-tanzania-init-vendor-callback", exchange);
                                }
                            });
                        }

                        // Update cash in log
                        /*String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                        if (cashInLogId != null) {
                            cashInLogService.findById(Long.parseLong(cashInLogId)).ifPresent(cashInLog -> {
                                cashInLog.setStatus(response.isResponseStatus() ? RequestStatus.SUCCESS : RequestStatus.FAILED);
                                cashInLog.setCashInResponse(responseBody);
                                cashInLogService.recordLog(cashInLog);
                            });
                        }*/
                    } else {
                        throw new IllegalStateException("Empty response body received from payment gateway");
                    }
                })
                .log("Response Status: ${exchangeProperty.responseStatus}, Code: ${exchangeProperty.responseCode}")
                .end();


        // Failed Transactions Route
        from("direct:failedTransactions")
                .id("failedTransactionsRoute")
                .log(LoggingLevel.ERROR, "Failed to process transaction: ${body}")
                .process(exchange -> {
                    // Store failed transaction for retry
                    // storeFailedTransaction(exchange);
                });

        // Operator Error Response Handler
        from("direct:handleFailure")
                .id("failureResponseRoute")
                .process(exchange -> {
                    BillerPaymentResponse response = exchange.getMessage().getBody(BillerPaymentResponse.class);
                    String errorDescription = response.getResponseDescription();

                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        cashInLog.setStatus(RequestStatus.OPERATOR_ERROR_RESPONSE);
                        //cashInLog.setPaymentReference(exchange.getProperty("reference", String.class));
                        cashInLogService.recordLog(cashInLog);
                    }

                    // Update deposit transaction
                    String transactionId = exchange.getIn().getHeader("transactionId", String.class);
                    depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                        deposit.setRequestStatus(RequestStatus.OPERATOR_ERROR_RESPONSE);
                        deposit.setErrorMessage(errorDescription);
                        depositService.recordDeposit(deposit);
                    });
                })
                .log(LoggingLevel.ERROR, "Operator error response: ${body.responseDescription}");

        // Vendor Callback route
        from("direct:mixx-tanzania-init-vendor-callback")
                .routeId("mixx-tanzania-init-vendor-callback-route")
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
                        CamelConfiguration.RABBIT_PRODUCER_TIGOPESA_INITS_URI,
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

    private <T> T shouldRetryCheck(Exchange exchange, Class<T> tClass) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean isRetryable = this.scoopExceptionHandlers.isRetryableException(exception);

        Integer retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
        if (retryCount == null) retryCount = 0;

        // Check if exception is not retryable - handle immediately
        if (!isRetryable) {
            log.info("Exception {} is non-retryable - stopping retries",
                    exception.getClass().getSimpleName());
            updateStatusForNonRetryableException(exchange, exception);

            if (tClass == Boolean.class) {
                return tClass.cast(false);
            }
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

        // return shouldRetry;

        if (tClass == Boolean.class) {
            return tClass.cast(shouldRetry);
        }

        throw new IllegalArgumentException("Unsupported type: " + tClass);
    }


    // Helper method to update status for non-retryable exceptions immediately
    private void updateStatusForNonRetryableException(Exchange exchange, Exception exception) {
        RequestStatus status = this.scoopExceptionHandlers.classifyException(exception);
        String errorPrefix = "Mixx payment failed (non-retryable): ";

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
            case NETWORK_CONNECTION_ISSUE -> "Could not connect to Mixx servers";
            case TIMEOUT -> "Mixx service did not respond in time";
            case SERVICE_UNAVAILABLE -> responseCode != null && responseCode == 429
                    ? "Too many requests - please try again later"
                    : "Mixx service is currently unavailable";
            case INVALID_CREDENTIALS -> "Authentication failed with Mixx service";
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
                    case NETWORK_CONNECTION_ISSUE -> "Could not connect to Mixx servers";
                    case TIMEOUT -> "Mixx service did not respond in time";
                    case SERVICE_UNAVAILABLE -> "Mixx service is currently unavailable";
                    case INVALID_CREDENTIALS -> "Authentication failed with Mixx service";
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
