package com.hybrid9.pg.Lipanasi.route.tqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hybrid9.pg.Lipanasi.component.mixxtqs.TigoResponseParser;
import com.hybrid9.pg.Lipanasi.dto.tqs.EPGQueryStatusResponse;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.HaloPesaConfig;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.mixxtqs.TigoTransactionService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

//@Component
public class EPGQueryStatusRoute extends RouteBuilder {

    private final PushUssdService pushUssdService;
    private final TigoResponseParser tigoResponseParser;
    private final TigoTransactionService tigoTransactionService;
    private final DepositService depositService;
    private final SessionManagementService sessionManagementService;

    public EPGQueryStatusRoute(PushUssdService pushUssdService, TigoResponseParser tigoResponseParser,
                               TigoTransactionService tigoTransactionService,
                               DepositService depositService,
                               SessionManagementService sessionManagementService) {
        this.pushUssdService = pushUssdService;
        this.tigoResponseParser = tigoResponseParser;
        this.tigoTransactionService = tigoTransactionService;
        this.depositService = depositService;
        this.sessionManagementService = sessionManagementService;
    }

    @Override
    public void configure() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        //Error Handler
        onException(Exception.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000) // 1 second delay between retries
                .backOffMultiplier(2)  // Exponential backoff
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logRetryAttempted(true)
                .logStackTrace(true)
                .end();

        // Main route to process NEW transactions
        from("quartz://tqs/halopesa?cron=0+0/50+*+*+*+?") // Run every 5 minutes
                .routeId("halopesa-query-transactions")
                .transacted()
                .process(exchange -> {
                    List<PushUssd> transactions = this.pushUssdService.getNewTransactions("Halopesa-Tanzania");
                    if (transactions != null && !transactions.isEmpty()) {
                        exchange.getIn().setBody(transactions);
                    } else {
                        // Skip processing if no transactions
                        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
                    }
                })
                .split(body())
                .toD("direct:process-halopesa-tqs-transactions");

        // Process individual transaction
        from("direct:process-halopesa-tqs-transactions")
                .routeId("halopesa-process-tqs-transaction")
                .process(exchange -> {
                    PushUssd transaction = exchange.getIn().getBody(PushUssd.class);
                    if (transaction != null) {
                        String referenceId = transaction.getReference();
                        ObjectNode objectNode = createJsonPayload(transaction, referenceId);
                        exchange.getIn().setBody(mapper.writeValueAsString(objectNode));
                        exchange.setProperty("originalTransaction", transaction);
                        // Store original record ID for later use
                        exchange.setProperty("recordId", transaction.getId());
                        exchange.setProperty("transactionNumber", transaction.getTransactionNumber());
                        exchange.setProperty("referenceId", transaction.getReference());
                        exchange.setProperty("amount", transaction.getAmount());
                        exchange.setProperty("msisdn", transaction.getMsisdn());
                        exchange.setProperty("networkConfig", (HaloPesaConfig) this.sessionManagementService.getSession(depositService.findByReference(transaction.getReference()).orElseThrow(()->new CustomExcpts.TransactionNotFoundException("Transaction Not found with Reference: "+transaction.getReference())).getSessionId()).orElseThrow(()->
                                new RuntimeException("Session not found for ID: " + depositService.findByReference(transaction.getReference()).orElseThrow(()->new CustomExcpts.TransactionNotFoundException("Transaction Not found with Reference: "+transaction.getReference())).getSessionId())).getNetworkConfig());
                    } else {
                        // Skip processing this null transaction
                        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
                    }
                })
                .toD("direct:update-halopesa-tqs-query-attempts")
                .removeHeaders("*")
                .setHeader("Accept", constant("application/json"))
                .setHeader("Content-Type", constant("application/json"))
                .log("Sending Halopesa Query Status Request: ${body}")
                .toD("${exchangeProperty.networkConfig.tqsUrl}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .convertBodyTo(String.class)
                .log("Received Halopesa Query Status Response: ${body}")
                .unmarshal().json(JsonLibrary.Jackson, EPGQueryStatusResponse.class)
                .process(exchange -> {
                    EPGQueryStatusResponse response = exchange.getIn().getBody(EPGQueryStatusResponse.class);
                    Long recordId = exchange.getProperty("recordId", Long.class);
                    String transactionNumber = exchange.getProperty("transactionNumber", String.class);

                    // Determine new status
                    String newStatus = determineCollectionStatus(response.getBody().getResponse().getResponseStatus());

                    //Set header for transaction status
                    exchange.setProperty("transactionStatus", newStatus);
                })
                .choice()
                .when(simple("${header.transactionStatus} == 'COLLECTED'"))
                .log(LoggingLevel.INFO, "Transaction Status: ${header.transactionStatus}")
                .toD("direct:update-halopesa-tqs-collected-transactions")
                .when(simple("${header.transactionStatus} == 'FAILED'"))
                .log(LoggingLevel.INFO, "Transaction Status: ${header.transactionStatus}")
                .toD("direct:update-halopesa-tqs-failed-transactions")
                .when(simple("${header.transactionStatus} == 'NEW'"))
                .log(LoggingLevel.INFO, "Transaction Status: ${header.transactionStatus}")
                .toD("direct:update-halopesa-tqs-new-transactions")
                .otherwise()
                .log(LoggingLevel.INFO, "No transaction found, response status was  : ${header.transactionStatus}")
                .end();

        // Update query attempts
        from("direct:update-halopesa-tqs-query-attempts")
                .routeId("update-halopesa-tqs-query-attempts")
                .process(exchange -> {
                    this.pushUssdService.findPushUssdById(exchange.getProperty("recordId", Long.class)).ifPresent(pushUssd -> {
                        pushUssd.setQueryAttempts(pushUssd.getQueryAttempts() + 1);
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .end();

        from("direct:update-halopesa-tqs-collected-transactions")
                .routeId("update-halopesa-collected-status-route")
                .process(exchange -> {
                    this.pushUssdService.findPushUssdById(exchange.getProperty("recordId", Long.class)).ifPresent(pushUssd -> {
                        pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
                        pushUssd.setStatus("0");
                        pushUssd.setEvent("Success");
                        pushUssd.setMessage("Success");
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .end();


        from("direct:update-halopesa-tqs-failed-transactions")
                .routeId("update-halopesa-failed-status-route")
                .process(exchange -> {
                    this.pushUssdService.findPushUssdById(exchange.getProperty("recordId", Long.class)).ifPresent(pushUssd -> {
                        pushUssd.setCollectionStatus(CollectionStatus.FAILED);
                        pushUssd.setStatus("-1");
                        pushUssd.setEvent("failed");
                        pushUssd.setMessage("Failed");
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .end();

        from("direct:update-halopesa-tqs-new-transactions")
                .routeId("update-halopesa-new-status-route")
                .process(exchange -> {
                    this.pushUssdService.findPushUssdById(exchange.getProperty("recordId", Long.class)).ifPresent(pushUssd -> {
                        pushUssd.setCollectionStatus(CollectionStatus.FAILED);
                        pushUssd.setStatus("-1");
                        pushUssd.setEvent("failed");
                        pushUssd.setMessage("Transaction Status Remained in  ${header.transactionStatus} Status, No action was executed");
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .end();

    }

    private ObjectNode createJsonPayload(PushUssd transaction, String referenceId) throws NoSuchAlgorithmException {
        ObjectMapper mapper = new ObjectMapper();
        // Construct Query Status request payload

        String amount = validateAmount(Float.parseFloat(String.valueOf(transaction.getAmount())));
        String msisdn = validateField(transaction.getMsisdn(), "MSISDN");
        String reference = transaction.getReference();
        String transactionNumber = transaction.getTransactionNumber();

        // Prepare header
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode header = mapper.createObjectNode();
        header.put("spId", "${exchangeProperty.networkConfig.spId}");
        header.put("spPassword", generateSpPassword());
        header.put("timestamp", getCurrentTimestamp());
        header.put("merchantCode", validateField("${exchangeProperty.networkConfig.merchantCode}", "Merchant Code"));

        // Prepare body
        ObjectNode bodyNode = mapper.createObjectNode();
        ObjectNode request = mapper.createObjectNode();
        request.put("command", "QueryStatus");
        request.put("command1", "UssdPush");
        request.put("reference", reference);
        request.put("amount", amount);
        request.put("currency", "TZS");
        request.put("msisdn", msisdn);
        request.put("transactionID", transactionNumber);

        bodyNode.put("request", request);

        rootNode.put("header", header);
        rootNode.put("body", bodyNode);
        return rootNode;
    }

    // Helper method to generate SP Password
    private String generateSpPassword() throws NoSuchAlgorithmException {

        // Create timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Generate spPassword with validated values
        String rawPassword = validateField("${exchangeProperty.networkConfig.spId}", "SP ID") +
                validateField("${exchangeProperty.networkConfig.secretKey}", "Secret Key") +
                timestamp;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawPassword.getBytes());
        return Base64.encodeBase64String(hash);
    }

    // Helper method to get current timestamp
    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    // Determine collection status based on response
    private String determineCollectionStatus(String responseStatus) {
        if ("Transaction Status: Completed".equals(responseStatus)) {
            return "COLLECTED";
        } else if ("Transaction Status: Failed".equals(responseStatus)) {
            return "FAILED";
        }
        return "NEW"; // Default case
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
}
