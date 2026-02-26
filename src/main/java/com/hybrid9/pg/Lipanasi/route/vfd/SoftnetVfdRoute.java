package com.hybrid9.pg.Lipanasi.route.vfd;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.VatInitialRequestDto;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.tax.TransactionTax;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import com.hybrid9.pg.Lipanasi.route.handler.exceptions.ScoopExceptionHandlers;
import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.SoftnetCircuitBreakerProcessor;
import com.hybrid9.pg.Lipanasi.services.CircuitBreakerService;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.tax.TransactionTaxService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

//@Component
public class SoftnetVfdRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SoftnetVfdRoute.class);

    @Value("${softnet.api.baseUrl}")
    private String baseUrl;

    @Value("${softnet.api.username}")
    private String username;

    @Value("${softnet.api.password}")
    private String password;

    @Value("${softnet.api.tinNumber}")
    private String tinNumber;

    @Value("${softnet.api.timeout:30000}")
    private int httpTimeout;

    @Autowired
    @Qualifier("softnetHttpClient")
    private HttpClient softnetHttpClient;

    @Autowired
    @Qualifier("softnetCircuitBreaker")
    private CircuitBreaker softnetCircuitBreaker;

    private final ServiceNameComponent serviceNameComponent;
    private final CircuitBreakerService circuitBreakerService;
    private final CashInLogService cashInLogService;
    private final TransactionTaxService transactionTaxService;
    private final ScoopExceptionHandlers scoopExceptionHandlers;

    public SoftnetVfdRoute(ServiceNameComponent serviceNameComponent,
                           CircuitBreakerService circuitBreakerService,
                           CashInLogService cashInLogService,
                           TransactionTaxService transactionTaxService,
                           ScoopExceptionHandlers scoopExceptionHandlers) {
        this.serviceNameComponent = serviceNameComponent;
        this.circuitBreakerService = circuitBreakerService;
        this.cashInLogService = cashInLogService;
        this.scoopExceptionHandlers = scoopExceptionHandlers;
        this.transactionTaxService = transactionTaxService;
    }

    @Override
    public void configure() throws Exception {
        // Register HTTP clients
        getContext().getRegistry().bind("softnetHttpClient", softnetHttpClient);

        // Set service name
        serviceNameComponent.setServiceName(ServiceName.SOFTNET_VFD);

        // Create circuit breaker processor
        SoftnetCircuitBreakerProcessor softnetCircuitProcessor = new SoftnetCircuitBreakerProcessor(
                softnetCircuitBreaker,
                transactionTaxService,
                serviceNameComponent
        );

        // Register Processor
        getContext().getRegistry().bind("softnetCircuitProcessor", softnetCircuitProcessor);

        // Configure comprehensive error handler with retry policy
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(2000) // 2 seconds initial delay
                .backOffMultiplier(2)
                .maximumRedeliveryDelay(10000) // Max 10 seconds
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retryWhile(this::shouldRetryCheck)
                .onRedelivery(exchange -> {
                    String transactionTaxId = exchange.getProperty("transactionTaxId", String.class);
                    if (transactionTaxId != null) {
                        TransactionTax transactionTax = transactionTaxService.findById(Long.parseLong(transactionTaxId)).orElse(null);
                        if (transactionTax != null) {
                            int retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
                            transactionTax.setRetryCount(retryCount);

                            if (retryCount < 3) {
                                transactionTax.setStatus(RequestStatus.MARKED_FOR_RETRY);
                                logger.warn("Softnet VFD retry attempt {} for invoice: {}",
                                        retryCount, exchange.getProperty("invoiceNumber"));
                            } else {
                                transactionTax.setStatus(RequestStatus.FAILED);
                                transactionTax.setErrorMessage("Maximum retry attempts reached - VFD transaction failed");
                                logger.error("Softnet VFD max retries reached for invoice: {}",
                                        exchange.getProperty("invoiceNumber"));
                            }
                            transactionTaxService.recordTax(transactionTax);
                        }
                    }
                })
        );

        /**
         * Main VFD Transaction Route
         */
        from(CamelConfiguration.RABBIT_CONSUMER_VAT_URI)
                .routeId("softnet-vfd-main-transaction")
                .log("Starting VFD transaction processing for invoice: ${header.invoiceNumber}")
                .process(exchange -> {
                    // Validate required headers
                    validateRequiredHeaders(exchange);

                    // Create initial log entry
                    TransactionTax transactionTax = TransactionTax.builder()
                            .vfdRequest(buildLogRequest(exchange))
                            .retryCount(0)
                            .status(RequestStatus.INITIATED)
                            .paymentReference(exchange.getIn().getHeader("invoiceNumber", String.class))
                            .build();

                    transactionTaxService.recordTax(transactionTax);
                    exchange.setProperty("transactionTaxId", transactionTax.getId());
                })
                .process("softnetCircuitProcessor")
                .doTry()
                // First get the token
                .to("direct:getTokenWithRetry")
                // Then push the invoice
                .to("direct:pushInvoiceWithRetry")
                .process(exchange -> {
                    // Record successful call to circuit breaker
                    circuitBreakerService.recordSuccess("softnetVfdService");
                    updateLogStatus(exchange, RequestStatus.COMPLETED, "VFD transaction completed successfully");
                })
                .log("VFD transaction completed successfully for invoice: ${header.invoiceNumber}")
                .endDoTry()
                .doCatch(Exception.class)
                .process(exchange -> handleTransactionException(exchange))
                .log(LoggingLevel.ERROR, "VFD transaction failed for invoice ${header.invoiceNumber}: ${exception.message}")
                .end();

        /**
         * Step 1: Get Token with Enhanced Error Handling
         */
        from("direct:getTokenWithRetry")
                .routeId("softnet-getToken-enhanced")
                .log("Requesting VFD authentication token for invoice: ${header.invoiceNumber}")
                .doTry()
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader("CamelHttpClient", constant(softnetHttpClient))
                .setHeader("CamelHttpConnectTimeout", constant(httpTimeout))
                .setHeader("CamelHttpSocketTimeout", constant(httpTimeout))
                .process(exchange -> {
                    // Build authentication request body
                    String authBody = String.format("""
                                {
                                  "userName": "%s",
                                  "password": "%s",
                                  "tinNumber": "%s"
                                }
                                """, username, password, tinNumber);
                    exchange.getIn().setBody(authBody);
                })
                .log("[VFD AUTH REQUEST] Invoice: ${header.invoiceNumber} -> ${body}")
                .toD(baseUrl + "/GetToken?httpMethod=POST&bridgeEndpoint=true")
                .convertBodyTo(String.class)
                .log("[VFD AUTH RESPONSE] ${body}")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .process(exchange -> {
                    // Parse JSON response to extract token
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        String responseBody = exchange.getIn().getBody(String.class);
                        var jsonNode = mapper.readTree(responseBody);

                        if (jsonNode.has("access_token") && !jsonNode.get("access_token").isNull()) {
                            String token = jsonNode.get("access_token").asText();
                            exchange.getIn().setHeader("VFDToken", token);
                            logger.debug("Successfully obtained VFD token for invoice: {}",
                                    exchange.getIn().getHeader("invoiceNumber"));
                        } else {
                            throw new IllegalStateException("No access_token found in VFD response");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse VFD token response: {}", e.getMessage());
                        throw new RuntimeException("Invalid VFD token response format", e);
                    }
                })
                .otherwise()
                .process(exchange -> {
                    Integer responseCode = exchange.getIn().getHeader("CamelHttpResponseCode", Integer.class);
                    String errorMsg = String.format("VFD authentication failed with HTTP %d", responseCode);
                    throw new HttpClientErrorException(
                            org.springframework.http.HttpStatus.valueOf(responseCode),
                            errorMsg
                    );
                })
                .end()
                .endDoTry()
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    logger.error("VFD token request failed: {}", exception.getMessage());

                    // Record circuit breaker failure for service-related issues
                    if (scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
                        circuitBreakerService.recordFailure("softnetVfdService", exception);
                    }

                    throw exception; // Re-throw to trigger retry mechanism
                })
                .end();

        /**
         * Step 2: Push Invoice Data with Enhanced Error Handling
         */
        from("direct:pushInvoiceWithRetry")
                .routeId("softnet-pushInvoice-enhanced")
                .log("Pushing VFD invoice data for invoice: ${header.invoiceNumber}")
                .doTry()
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader("Authorization", simple("Bearer ${header.VFDToken}"))
                .setHeader("CamelHttpClient", constant(softnetHttpClient))
                .setHeader("CamelHttpConnectTimeout", constant(httpTimeout))
                .setHeader("CamelHttpSocketTimeout", constant(httpTimeout))
                .process(exchange -> {
                    // Build invoice request body
                    String invoiceBody = buildInvoiceRequestBody(exchange);
                    exchange.getIn().setBody(invoiceBody);
                })
                .log("[VFD TRANSACTION REQUEST] ${body}")
                .toD(baseUrl + "/PostTransaction?httpMethod=POST&bridgeEndpoint=true")
                .convertBodyTo(String.class)
                .log("[VFD TRANSACTION RESPONSE] ${body}")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .process(exchange -> {
                    // Validate transaction response
                    validateTransactionResponse(exchange);
                })
                .otherwise()
                .process(exchange -> {
                    Integer responseCode = exchange.getIn().getHeader("CamelHttpResponseCode", Integer.class);
                    String responseBody = exchange.getIn().getBody(String.class);
                    String errorMsg = String.format("VFD transaction failed with HTTP %d: %s",
                            responseCode, responseBody);
                    throw new HttpServerErrorException(
                            org.springframework.http.HttpStatus.valueOf(responseCode),
                            errorMsg
                    );
                })
                .end()
                .endDoTry()
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    logger.error("VFD invoice push failed: {}", exception.getMessage());

                    // Record circuit breaker failure for service-related issues
                    if (scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
                        circuitBreakerService.recordFailure("softnetVfdService", exception);
                    }

                    throw exception; // Re-throw to trigger retry mechanism
                })
                .end();

        /**
         * Health Check Route
         */
        from("direct:softnetVfdHealthCheck")
                .routeId("softnet-vfd-health-check")
                .log("Performing Softnet VFD health check")
                .doTry()
                .setHeader("testInvoiceNumber", constant("HEALTH_CHECK_" + System.currentTimeMillis()))
                .setHeader("amount", constant("100"))
                .setHeader("randomId", constant("999999"))
                .to("direct:getTokenWithRetry")
                .setBody(constant("VFD Service is healthy"))
                .endDoTry()
                .doCatch(Exception.class)
                .setBody(constant("VFD Service is unhealthy"))
                .setHeader("healthCheckError", simple("${exception.message}"))
                .end();
    }

    /**
     * Validates required headers for VFD transaction
     */
    private void validateRequiredHeaders(Exchange exchange) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        VatInitialRequestDto vatInitialRequestDto = mapper.readValue(exchange.getIn().getBody(String.class), VatInitialRequestDto.class);

        String invoiceNumber = vatInitialRequestDto.getPaymentReference(); //exchange.getIn().getHeader("invoiceNumber", String.class);
        String amount = String.valueOf(vatInitialRequestDto.getAmount());
        String randomId = vatInitialRequestDto.getSessionId(); //exchange.getIn().getHeader("randomId", String.class);

        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (randomId == null || randomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Random ID is required");
        }

        // Validate amount is numeric
        try {
            Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number: " + amount);
        }
    }

    /**
     * Builds invoice request body with proper escaping
     */
    private String buildInvoiceRequestBody(Exchange exchange) {
        String token = exchange.getIn().getHeader("VFDToken", String.class);
        String invoiceNumber = exchange.getIn().getHeader("invoiceNumber", String.class);
        String amount = exchange.getIn().getHeader("amount", String.class);
        String randomId = exchange.getIn().getHeader("randomId", String.class);

        return String.format("""
                {
                  "token": "%s",
                  "paymenttype": 5,
                  "customer_id_type": 6,
                  "customer_name": "WIA",
                  "mobile_number": "8686535329",
                  "customer_id_number": "",
                  "reference_number": "%s",
                  "operator_name": "operator 001",
                  "vrn": "VRN 001",
                  "receipt_items": [
                      {
                          "id": %s,
                          "taxCode": 1,
                          "quantity": 1.0,
                          "paidAmount": "%s",
                          "description": "WIA Internet Bill",
                          "discount": 0,
                          "unitPrice": "%s"
                      }
                  ]
                }
                """, token, invoiceNumber, randomId, amount, amount);
    }

    /**
     * Validates transaction response from VFD service
     */
    private void validateTransactionResponse(Exchange exchange) {
        String responseBody = exchange.getIn().getBody(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            var jsonNode = mapper.readTree(responseBody);

            // Check for success indicators in response
            if (jsonNode.has("success")) {
                boolean success = jsonNode.get("success").asBoolean();
                if (!success) {
                    String message = jsonNode.has("message") ?
                            jsonNode.get("message").asText() : "Transaction failed";
                    throw new RuntimeException("VFD transaction failed: " + message);
                }
            }

            // Log successful transaction details
            String receiptNumber = jsonNode.has("receiptNumber") ?
                    jsonNode.get("receiptNumber").asText() : "N/A";
            logger.info("VFD transaction successful - Receipt: {}, Invoice: {}",
                    receiptNumber, exchange.getIn().getHeader("invoiceNumber"));

        } catch (Exception e) {
            logger.error("Failed to validate VFD transaction response: {}", e.getMessage());
            throw new RuntimeException("Invalid VFD transaction response", e);
        }
    }

    /**
     * Builds log request string for audit purposes
     */
    private String buildLogRequest(Exchange exchange) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var logData = mapper.createObjectNode();
            logData.put("invoiceNumber", exchange.getIn().getHeader("invoiceNumber", String.class));
            logData.put("amount", exchange.getIn().getHeader("amount", String.class));
            logData.put("randomId", exchange.getIn().getHeader("randomId", String.class));
            logData.put("timestamp", System.currentTimeMillis());
            return mapper.writeValueAsString(logData);
        } catch (Exception e) {
            return "Failed to serialize log request: " + e.getMessage();
        }
    }

    /**
     * Determines if an exception should trigger a retry
     */
    private <T> T shouldRetryCheck(Exchange exchange, Class<T> tClass) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        boolean shouldRetry = scoopExceptionHandlers.isRetryableException(exception);

        if (!shouldRetry) {
            logger.info("Exception {} is non-retryable for invoice {}, skipping retry",
                    exception.getClass().getSimpleName(),
                    exchange.getProperty("invoiceNumber"));
            updateStatusForNonRetryableException(exchange, exception);
        }

        if (tClass == Boolean.class) {
            return tClass.cast(shouldRetry);
        }

        throw new IllegalArgumentException("Unsupported type: " + tClass);
    }

    /**
     * Handles transaction exceptions with proper classification
     */
    private void handleTransactionException(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        RequestStatus status = scoopExceptionHandlers.classifyException(exception);
        String errorPrefix = "Softnet VFD transaction failed: ";

        // Record circuit breaker failure for service-related issues
        if (!(exception instanceof SoftnetCircuitBreakerProcessor.CircuitBreakerOpenException)
                && scoopExceptionHandlers.shouldTriggerCircuitBreaker(exception)) {
            circuitBreakerService.recordFailure("softnetVfdService", exception);
        }

        updateLogStatus(exchange, status, errorPrefix + exception.getMessage());
    }

    /**
     * Updates status for non-retryable exceptions
     */
    private void updateStatusForNonRetryableException(Exchange exchange, Exception exception) {
        RequestStatus status = scoopExceptionHandlers.classifyException(exception);
        String errorPrefix = "Softnet VFD failed (non-retryable): ";
        updateLogStatus(exchange, status, errorPrefix + exception.getMessage());
    }

    /**
     * Updates cash-in log status
     */
    private void updateLogStatus(Exchange exchange, RequestStatus status, String message) {
        String transactionTaxId = exchange.getProperty("transactionTaxId", String.class);
        if (transactionTaxId != null) {
            TransactionTax transactionTax = transactionTaxService.findById(Long.parseLong(transactionTaxId)).orElse(null);
            if (transactionTax != null) {
                transactionTax.setStatus(status);
                transactionTax.setErrorMessage(message);
                transactionTaxService.recordTax(transactionTax);
            }
        }
    }
}
