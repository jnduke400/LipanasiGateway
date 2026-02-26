package com.hybrid9.pg.Lipanasi.route;

import jakarta.xml.bind.DatatypeConverter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//@Component
public class UserValidation extends RouteBuilder {
    private static final String API_BASE_URL = "https://mbet-astra-api.negroup-tech.net/pps";
    private static final String MERCHANT_ID = "2hMDwxphhOCjHjVDPWkFrPM5ugH3dsEhFwB";
    private static final String API_SECRET_KEY = "jTFNXu8w7hQGOighyFB8reGtxAIq8pNwcjf";
    private static final String CUSTOMER_REGISTRATION_URL = "https://mbet-tz-prod-sb-feapi.negroup-tech.net/api/v1/register";
    @Override
    public void configure() throws Exception {

        // Global exception handler
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Error in payment flow: ${exception.message}")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"Payment process failed\"}"));

        // HTTP operation failure handler
        onException(HttpOperationFailedException.class)
                .handled(true)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .logStackTrace(true)
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"HTTP request failed after 3 retries\"}"));

        // Main route
        from("direct:processUSSDPayment")
                .log("Starting USSD payment process for user ${header.userName}")
                // Step 1: Validate User
                .to("direct:validateUser")
                .choice()
                .when(simple("${body[status]} == 200"))
                .log("User validation successful, proceeding with deposit")
                .to("direct:initiateDeposit")
                .otherwise()
                .log("User validation failed: ${body[message]}")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"${body[message]}\"}"))
                .end();

        // User validation route
        from("direct:validateUser")
                .doTry()
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("merchantId", constant(MERCHANT_ID))
                .setHeader(Exchange.HTTP_URI, simple(API_BASE_URL + "/agent/player-details-by-username?userName=${header.userName}"))
                .to("http://dummy")
                .unmarshal().json(JsonLibrary.Jackson)
                .process(validateUserProcessor())
                .doCatch(HttpOperationFailedException.class)
                .process(exchange -> {
                    System.out.println("Error in validateUserProcessor: " + exchange.getIn().getBody(Map.class));
                    // exchange.getIn().setBody(simple("{\"status\": \"ERROR\", \"message\": \"HTTP request failed after 3 retries\"}"));

                })
                .to("direct:registerCustomer")
                .end();

        // Deposit initiation route
        from("direct:initiateDeposit")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("merchantId", constant(MERCHANT_ID))
                .setHeader(Exchange.HTTP_URI, simple(API_BASE_URL + "/payment/deposit"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .process(exchange -> {
                    String timestamp = getCurrentTimestamp();
                    String transactionId = generateTransactionId();
                    exchange.setProperty("timestamp", timestamp);
                    exchange.setProperty("transactionId", transactionId);

                    // Create deposit request body
                    String depositBody = String.format(
                            "{\"accountId\":%s,\"amount\":%s,\"ipAddress\":\"%s\",\"modePaymentClient\":\"USSD\"," +
                                    "\"promoCode\":\"\",\"signature\":\"%s\",\"timeStamp\":\"%s\",\"transactionId\":\"%s\"}",
                            exchange.getIn().getHeader("accountId"),
                            exchange.getIn().getHeader("amount"),
                            exchange.getIn().getHeader("ipAddress"),
                            calculateSignature(exchange),
                            timestamp,
                            transactionId
                    );
                    exchange.getIn().setBody(depositBody);
                })
                .to("http://dummy")
                .unmarshal().json(JsonLibrary.Jackson)
                .choice()
                .when(simple("${body[status]} == 200"))
                .log("Deposit initiated successfully, proceeding with confirmation")
                .to("direct:confirmDeposit")
                .otherwise()
                .log("Deposit initiation failed")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"Deposit initiation failed\"}"))
                .end();

        // Deposit confirmation route
        from("direct:confirmDeposit")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("merchantId", constant(MERCHANT_ID))
                .setHeader(Exchange.HTTP_URI, simple(API_BASE_URL + "/payment/deposit/${body[data][transactionId]}"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .process(exchange -> {
                    String timestamp = getCurrentTimestamp();
                    // Create confirmation request body
                    String confirmBody = String.format(
                            "{\"accountId\":%s,\"message\":\"success\",\"modePaymentClient\":\"USSD\"," +
                                    "\"paymentMethodType\":\"APP\",\"signature\":\"%s\",\"status\":\"CONFIRMED\"," +
                                    "\"timeStamp\":\"%s\",\"transactionRef\":\"%s\"}",
                            exchange.getIn().getHeader("accountId"),
                            calculateSignature(exchange),
                            timestamp,
                            exchange.getProperty("transactionId")
                    );
                    exchange.getIn().setBody(confirmBody);
                })
                .to("http://dummy")
                .unmarshal().json(JsonLibrary.Jackson)
                .log("Deposit confirmation response: ${body}");

        //Customer Registration
        from("direct:registerCustomer")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Trace-Id", simple(UUID.randomUUID().toString()))
                .setHeader(Exchange.HTTP_URI, simple(CUSTOMER_REGISTRATION_URL + "?bridgeEndpoint=true&httpMethod=POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .process(exchange -> {
                    String timestamp = getCurrentTimestamp();
                    // Create registration request body
                    String depositBody = String.format(
                            "{\n" +
                                    "  \"clientType\": \"mobile\",\n" +
                                    "  \"countryCode\": \"TZ\",\n" +
                                    "  \"description\": \"%s\",\n" +
                                    "  \"mobileNumber\": \"%s\",\n" +
                                    "  \"password\": \"%s\",\n" +
                                    "  \"role\": \"PLAYER\",\n" +
                                    "  \"termsAndConditions\": true\n" +
                                    "}",
                            generateUrlSafeBase64(exchange.getIn().getHeader("userName",String.class)),
                            exchange.getIn().getHeader("userName"),
                            generateFourDigits()

                    );
                    exchange.getIn().setBody(depositBody);
                })
                .log("Customer registration request headers: ${headers}")
                .log("Customer registration request body: ${body}")
                .to("http://dummy")
                .unmarshal().json(JsonLibrary.Jackson)
                .choice()
                .when(simple("${body[result][status]} == 200"))
                .log("Customer registered successfully, proceeding with deposit ${exchangeProperty.transactionId}")
                .to("direct:initiateDeposit")
                .otherwise()
                .log("Customer registration failed")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"Customer registration failed\"}"))
                .end();
    }


    public String generateUrlSafeBase64(String mobileNumber) {
        byte[] encodedBytes = Base64.getUrlEncoder().encode(mobileNumber.getBytes(StandardCharsets.UTF_8));
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    public String generateFourDigits() {
        int number = (int) (Math.random() * 9000) + 1000;
        return String.valueOf(number);
    }

    private Processor validateUserProcessor() {
        return exchange -> {
            Map<String, Object> response = exchange.getIn().getBody(Map.class);

            // Check if response is null
            if (response == null) {
                exchange.getIn().setBody(createErrorResponse("Validation failed: Empty response"));
                return;
            }

            // Check if data array exists and is not empty
            Object dataObj = response.get("data");
            if (!(dataObj instanceof java.util.List) || ((java.util.List<?>) dataObj).isEmpty()) {
                exchange.getIn().setBody(createErrorResponse("Validation failed: User not found"));
                return;
            }

            // Get the first user from data array
            Map<String, Object> userData = (Map<String, Object>) ((java.util.List<?>) dataObj).get(0);

            // Check account status
            String accountStatus = (String) userData.get("accountStatus");
            if (accountStatus == null) {
                exchange.getIn().setBody(createErrorResponse("Validation failed: Account status not found"));
                return;
            }

            if (!"ACTIVE".equals(accountStatus)) {
                exchange.getIn().setBody(createErrorResponse("Validation failed: Account is not active"));
                return;
            }

            // If all validations pass, set the original successful response
            response.put("status", 200);
            response.put("message", "User validation successful");
            exchange.getIn().setBody(response);

            // Store user ID for later use
            exchange.setProperty("userId", userData.get("userId"));
        };
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 400);
        errorResponse.put("message", message);
        return errorResponse;
    }

    private String calculateSignature(Exchange exchange) {
        try {
            String userId = exchange.getIn().getHeader("accountId", String.class);
            String timestamp = exchange.getProperty("timestamp", String.class);

            // Concatenate the required fields
            String dataToSign = userId + timestamp + MERCHANT_ID + API_SECRET_KEY;

            // Create SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToSign.getBytes("UTF-8"));

            // Convert to hexadecimal
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    private String getCurrentTimestamp() {
        return DateTimeFormatter
                .ISO_INSTANT
                .format(Instant.now().atOffset(ZoneOffset.UTC));
    }

    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

