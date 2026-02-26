package com.hybrid9.pg.Lipanasi.route.tqs;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.AirtelMoneyTokenResponse;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.ServiceName;
import com.hybrid9.pg.Lipanasi.route.processor.ampaybill.AirtelResponseProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker.AirtelCircuitBreakerProcessor;
import com.hybrid9.pg.Lipanasi.services.CircuitBreakerService;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpMethods;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

//@Component
public class AirtelMoneyTQSRoute extends RouteBuilder {

    @Value("${payment-gateway.airtel_money.base-url}")
    private String airtelApiBaseUrl;

    @Value("${payment-gateway.airtel_money.token-url}")
    private String tokenUrl;

    @Value("${payment-gateway.airtel_money.tanzania.country}")
    private String airtelCountry;

    @Value("${payment-gateway.airtel_money.tanzania.currency}")
    private String airtelCurrency;

    /*@Value("${camel.batch.size}")
    private int batchSize;*/

    /*@Value("${camel.polling.delay}")
    private int pollingDelay;*/

    /*@Value("${camel.max.query.attempts}")
    private int maxQueryAttempts;*/

    private AirtelResponseProcessor airtelResponseProcessor;
    private final PushUssdService pushUssdService;
    private final ServiceNameComponent serviceNameComponent;
    private final CircuitBreakerService circuitBreakerService;
    private final CashInLogService cashInLogService;
    private final DepositService depositService;

    public AirtelMoneyTQSRoute(AirtelResponseProcessor airtelResponseProcessor, PushUssdService pushUssdService, ServiceNameComponent serviceNameComponent, CircuitBreakerService circuitBreakerService, CashInLogService cashInLogService, DepositService depositService) {
        this.airtelResponseProcessor = airtelResponseProcessor;
        this.pushUssdService = pushUssdService;
        this.serviceNameComponent = serviceNameComponent;
        this.circuitBreakerService = circuitBreakerService;
        this.cashInLogService = cashInLogService;
        this.depositService = depositService;
    }

    @Autowired
    @Qualifier("amTqsHttpClient")
    private HttpClient amTqsHttpClient;

    @Autowired
    @Qualifier("amTqsCircuitBreaker")
    private CircuitBreaker amTqsCircuitBreaker;

    @Override
    public void configure() throws Exception {

        // Register HTTP clients
        getContext().getRegistry().bind("amTqsHttpClient", amTqsHttpClient);

        serviceNameComponent.setServiceName(ServiceName.AM_TQS_PAYMENT);

        // Create circuit breaker processor
        AirtelCircuitBreakerProcessor amTqsCircuitProcessor = new AirtelCircuitBreakerProcessor(
                amTqsCircuitBreaker,
                cashInLogService,
                depositService,
                serviceNameComponent
        );

        //Register Processor
        getContext().getRegistry().bind("amTqsCircuitProcessor", amTqsCircuitProcessor);

        // Error handler
        errorHandler(deadLetterChannel("direct:am_tqs_errorHandler")
                .maximumRedeliveries(3)
                .redeliveryDelay(5000)
                .useOriginalMessage()
                .logRetryAttempted(true)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        // Error handling route
        from("direct:am_tqs_errorHandler")
                .log(LoggingLevel.ERROR, "Error processing record: ${exception.message}")
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    exchange.getIn().setHeader("errorMessage", exception.getMessage());
                })
                .log("Error Message when processing AM TQS: ${header.errorMessage}");

                //.to("sql:UPDATE c2b_push_ussd SET error_message = :?errorMessage, query_attempts = query_attempts + 1, last_modified_date = NOW() WHERE id = :?recordId");

        // Main route - Polls database for NEW records
        from("quartz://tqs/airtelTqsmoney?cron=0+0/50+*+*+*+?") // Run every 5 minutes
                .log(LoggingLevel.INFO, "Starting to poll for NEW transactions")
                .to("direct:fetchNewTransactions");

        from("direct:fetchNewTransactions")
                .routeId("airtel-money-tqs-query-transactions")
                .transacted()
                .process(exchange -> {
                    List<PushUssd> transactions = this.pushUssdService.getNewTransactionsTest("AirtelMoney-Tanzania");
                    if (transactions != null && !transactions.isEmpty()) {
                        exchange.getIn().setBody(transactions);
                    } else {
                        // Skip processing if no transactions
                        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
                    }
                })
                .split(body())
                .process(exchange -> {
                    PushUssd transaction = (PushUssd) exchange.getIn().getBody();
                    exchange.getIn().setHeader("recordId", transaction.getId());
                    exchange.getIn().setHeader("transactionId", transaction.getTransactionNumber());
                    exchange.getIn().setHeader("operator", transaction.getOperator());
                    exchange.getIn().setHeader("queryAttempts", transaction.getQueryAttempts());
                    this.cashInLogService.findByPaymentReference(transaction.getReference()).ifPresent(cashInLog -> {
                        exchange.setProperty("cashInLogId", cashInLog.getId().toString());
                    });
                    this.depositService.findByReference(transaction.getReference()).ifPresent(deposit -> {
                        exchange.setProperty("transactionId", deposit.getTransactionId());
                    });
                })
                .log(LoggingLevel.INFO, "Processing transaction with ID: ${header.recordId}")
                .to("direct:process-airtel-money-tqs-transaction")
                .end();

        // Process each transaction
        from("direct:process-airtel-money-tqs-transaction")
                .log(LoggingLevel.INFO, "Querying Airtel TQS for transaction: ${header.transactionId}")
                .to("direct:updateAmQueryAttempts")
                .to("direct:callAirtelTQS")
                .process(airtelResponseProcessor::process)
                .choice()
                .when(simple("${header.airtelTransactionStatus} == 'TS'"))
                .log(LoggingLevel.INFO, "Transaction successful: ${header.transactionId}")
                .to("direct:updateStatusCollected")
                .when(simple("${header.airtelTransactionStatus} == 'TF' || ${header.airtelTransactionStatus} == 'TE'"))
                .log(LoggingLevel.INFO, "Transaction failed: ${header.transactionId}")
                .to("direct:updateStatusFailed")
                .when(simple("${header.airtelTransactionStatus} == 'TIP'"))
                .log(LoggingLevel.INFO, "Transaction in progress: ${header.transactionId}")
                .to("direct:updateStatusNew")
                .when(simple("${exchangeProperty.processingError} == true"))
                .log(LoggingLevel.WARN, "Error processing transaction: ${header.errorMessage}")
                .to("direct:handleProcessingError")
                .otherwise()
                .log(LoggingLevel.WARN, "Unknown transaction status: ${header.airtelTransactionStatus}")
                .to("direct:handleUnknownStatus")
                .end();

        // Update query attempts
        from("direct:updateAmQueryAttempts")
                .routeId("update-am-tqs-query-attempts")
                .process(exchange -> {
                    PushUssd transaction = (PushUssd) exchange.getIn().getBody();
                    exchange.getIn().setHeader("recordId", transaction.getId());
                    exchange.getIn().setHeader("queryAttempts", transaction.getQueryAttempts());
                    this.pushUssdService.findPushUssdById(transaction.getId()).ifPresent(pushUssd -> {
                        pushUssd.setQueryAttempts(pushUssd.getQueryAttempts() + 1);
                        this.pushUssdService.update(pushUssd);
                    });

                })
                .end();

        // Call Airtel TQS API
        from("direct:callAirtelTQS")
                .routeId("airtel-money-tqs-api-call")
                .process("amTqsCircuitProcessor")
                .doTry()
                // First we generate Token
                .to("direct:getAirtelMoneyTqsToken")
                //then we remove all headers except the ones we need to keep
                .removeHeaders("*")
                // Then we set our specific headers
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.GET))
                .setHeader("Accept", constant("*/*"))
                .setHeader("X-Country", simple(airtelCountry))
                .setHeader("X-Currency", simple(airtelCurrency))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.auth_token}"))
                .toD(airtelApiBaseUrl + "/standard/v1/payments/${header.transactionId}")
                .convertBodyTo(String.class)
                .log(LoggingLevel.DEBUG, "Airtel TQS Response: ${body}")
                .setHeader("responseJson", simple("${body}"))
                .endDoTry()
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    // Skip recording failure for CircuitBreakerOpenException since it's already open
                    if (!(exception instanceof AirtelCircuitBreakerProcessor.CircuitBreakerOpenException)) {
                        // Record failure to circuit breaker
                        circuitBreakerService.recordFailure("amTqsPaymentService", exception);
                    }

                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        if (cashInLog != null) {
                            cashInLog.setStatus(RequestStatus.NETWORK_CONNECTION_ISSUE);
                            cashInLog.setErrorMessage("Error connecting to Airtel service: " + exception.getMessage());
                            cashInLogService.recordLog(cashInLog);
                        }
                    }

                    // Update deposit transaction
                    String transactionId = exchange.getProperty("transactionId", String.class);
                    if (transactionId != null) {
                        depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                            deposit.setRequestStatus(RequestStatus.NETWORK_CONNECTION_ISSUE);
                            deposit.setErrorMessage("Network connection issue with Airtel operator: TQS Service");
                            depositService.recordDeposit(deposit);
                        });
                    }
                })
                .log(LoggingLevel.ERROR, "Network connection issue encountered: ${exception.message}")
                .end();

        // Token Management Route
        from("direct:getAirtelMoneyTqsToken")
                .id("get-tqs-TokenRoute")
                // Store original body in a property
                .setProperty("originalBody", simple("${body}"))
                .setBody(constant("""
                        {
                            "client_id": "976cf4da-2f2e-4bf3-a54b-6d9e4031814d",
                            "client_secret": "d8540a64-2f47-49f8-b8e0-da6ba24cc617",
                            "grant_type": "client_credentials"
                        }
                        """))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("*/*"))
                .log("Airtel Money Token Request Body: ${body}")
                .to(tokenUrl + "?bridgeEndpoint=true&httpMethod=POST")
                .convertBodyTo(String.class)
                .log("Airtel Money Token Response: ${body}")
                //.unmarshal().json(JsonLibrary.Jackson)
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();
                    AirtelMoneyTokenResponse tokenResponse = mapper.readValue(exchange.getIn().getBody(String.class), AirtelMoneyTokenResponse.class);
                    exchange.getMessage().setHeader("auth_token", tokenResponse.getAccess_token());
                    // Restore original body
                    exchange.getMessage().setBody(exchange.getProperty("originalBody"));
                })
                .setProperty("auth_token", simple("${header.auth_token}"));

        // Update status to COLLECTED
        from("direct:updateStatusCollected")
                .routeId("update-collected-status-route")
                .process(exchange -> {
                    PushUssd transaction = (PushUssd) exchange.getIn().getBody();
                    this.pushUssdService.findPushUssdById(transaction.getId()).ifPresent(pushUssd -> {
                        pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
                        pushUssd.setStatus("0");
                        pushUssd.setEvent("Success");
                        pushUssd.setMessage("Success");
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .end();

        // Update status to FAILED
        from("direct:updateStatusFailed")
                .routeId("update-failed-status-route")
                .process(exchange -> {
                    PushUssd transaction = (PushUssd) exchange.getIn().getBody();
                    this.pushUssdService.findPushUssdById(transaction.getId()).ifPresent(pushUssd -> {
                        pushUssd.setCollectionStatus(CollectionStatus.FAILED);
                        pushUssd.setStatus("-1");
                        pushUssd.setEvent("failed");
                        pushUssd.setMessage("Failed");
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .end();

        // Keep status to NEW (for transactions in progress)
        from("direct:updateStatusNew")
                .routeId("keeping-new-status-route")
                .log(LoggingLevel.INFO, "Transaction in progress: ${header.transactionId}")
                .end();

        // Handle processing error
        from("direct:handleProcessingError")
                .routeId("handle-processing-error-route")
                .log(LoggingLevel.WARN, "Error processing transaction: ${header.errorMessage}")
                .end();

        // Handle unknown status
        from("direct:handleUnknownStatus")
                .routeId("handle-unknown-status-route")
                .log(LoggingLevel.WARN, "Unknown transaction status: ${header.airtelTransactionStatus}")
                .end();
    }
}
