package com.hybrid9.pg.Lipanasi.route.paybill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.PayBillPaymentDto;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.resources.ConstructorBuilder;
import com.hybrid9.pg.Lipanasi.route.processor.DepositProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillDeduplicationService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

//@Component
public class UserValidationRoute extends RouteBuilder {
    private static final String API_BASE_URL = "https://mbet-astra-api.negroup-tech.net/pps";
    private static final String MERCHANT_ID = "2hMDwxphhOCjHjVDPWkFrPM5ugH3dsEhFwB";
    private static final String API_SECRET_KEY = "jTFNXu8w7hQGOighyFB8reGtxAIq8pNwcjf";
    private static final String CUSTOMER_REGISTRATION_URL = "https://mbet-tz-prod-sb-feapi.negroup-tech.net/api/v1/register";

    private final ConstructorBuilder constructorBuilder;
    private final PayBillPaymentService payBillPaymentService;
    private final MnoServiceImpl mnoService;

    public UserValidationRoute(ConstructorBuilder constructorBuilder, PayBillPaymentService payBillPaymentService, MnoServiceImpl mnoService) {
        this.constructorBuilder = constructorBuilder;
        this.payBillPaymentService = payBillPaymentService;
        this.mnoService = mnoService;
    }

    private static void processData(Exchange exchange) {
        Map<String, Object> response = exchange.getIn().getBody(Map.class);

        // Get the result object
        Map<String, Object> result = (Map<String, Object>) response.get("result");

        // Get the data object from result
        Map<String, Object> data = (Map<String, Object>) result.get("data");

        // Get userId from data
        Integer userId = (Integer) data.get("userId");

        // Set userId in the exchange header
        exchange.getIn().setHeader("userId", userId);
    }

    @Override
    public void configure() throws Exception {
        // Global exception handler
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Error in payment flow: ${exception.message}")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"Payment process failed\"}"));

        // HTTP operation failure handler
       /* onException(HttpOperationFailedException.class)
                .handled(true)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .logStackTrace(true)
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"HTTP request failed after 3 retries\"}"));
*/
        // Main route for airtelmoney
        from(CamelConfiguration.RABBIT_CONSUMER_AIRTEL_MONEY_PAY_BILL_VALIDATION_URI)
                .log("Starting USSD payment process for user ${header.userName}")
                .bean(PayBillDeduplicationService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(this::process)
                // Step 1: Validate User
                .to("direct:validateUser")
                .choice()
                .when(simple("${body[status]} == 200"))
                .log("User validation successful, proceeding with deposit")
                // Convert to JSON
                .process(this::processJsonBody)
                .to(CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_PAY_BILL_URI)
                .otherwise()
                .log("User validation failed: ${body[message]}")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"${body[message]}\"}"))
                .end();


        // Main route for mixx by yas
        from(CamelConfiguration.RABBIT_CONSUMER_TIGOPESA_PAY_BILL_VALIDATION_URI)
                .log("Starting USSD payment process for user ${header.userName}")
                .bean(PayBillDeduplicationService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(this::process)
                // Step 1: Validate User
                .to("direct:validateUser")
                .choice()
                .when(simple("${body[status]} == 200"))
                .log("User validation successful, proceeding with deposit")
                // Convert to JSON
                .process(this::processJsonBody)
                .to(CamelConfiguration.RABBIT_PRODUCER_TIGOPESA_PAY_BILL_URI)
                .otherwise()
                .log("User validation failed: ${body[message]}")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"${body[message]}\"}"))
                .end();

        // Main route for mpesa
        from(CamelConfiguration.RABBIT_CONSUMER_MPESA_PAY_BILL_VALIDATION_URI)
                .log("Starting USSD payment process for user ${header.userName}")
                .bean(PayBillDeduplicationService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(this::process)
                // Step 1: Validate User
                .to("direct:validateUser")
                .choice()
                .when(simple("${body[status]} == 200"))
                .log("User validation successful, proceeding with deposit")
                // Convert to JSON
                .process(this::processJsonBody)
                .to(CamelConfiguration.RABBIT_PRODUCER_MPESA_PAY_BILL_URI)
                .otherwise()
                .log("User validation failed: ${body[message]}")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"${body[message]}\"}"))
                .end();


        // Main route for halopesa
        from(CamelConfiguration.RABBIT_CONSUMER_HALOPESA_PAY_BILL_VALIDATION_URI)
                .log("Starting USSD payment process for user ${header.userName}")
                .bean(PayBillDeduplicationService.class, "checkAndMarkDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(this::process)
                // Step 1: Validate User
                .to("direct:validateUser")
                .choice()
                .when(simple("${body[status]} == 200"))
                .log("User validation successful, proceeding with deposit")
                // Convert to JSON
                .process(this::processJsonBody)
                .to(CamelConfiguration.RABBIT_PRODUCER_HALOPESA_PAY_BILL_URI)
                .when(simple("${body[status]} == 400"))
                .log("User validation failed: ${body[message]}")
                //.setBody(simple("{\"status\": \"ERROR\", \"message\": \"${body[message]}\"}"))
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
                //.to(API_BASE_URL + "/agent/player-details-by-username?userName=${header.userName}")
                .to("http://dummy")
                .log("Validation Response Body >>>>>>>>>>: ${body}")
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
        from("direct:initiatePayBillDeposit")
                //.removeHeaders("*", "amount", "accountId", "ipAddress")
                // .setHeader(Exchange.HTTP_METHOD, constant("POST"))

                .process(exchange -> {
                    String timestamp = getCurrentTimestamp();
                    String transactionId = generateTransactionId();
                    exchange.setProperty("timestamp", timestamp);
                    exchange.setProperty("transactionId", transactionId);
                    exchange.getIn().setHeader("timestamp", timestamp);
                    exchange.getIn().setHeader("transactionId", transactionId);

                    // Create deposit request body
                    String depositBody = String.format(
                            "{\"accountId\":%s,\"amount\":%s,\"ipAddress\":\"%s\",\"modePaymentClient\":\"USSD\"," +
                                    "\"promoCode\":\"\",\"signature\":\"%s\",\"timeStamp\":\"%s\",\"transactionId\":\"%s\"}",
                            exchange.getIn().getHeader("userId"),
                            exchange.getIn().getHeader("amount"),
                            exchange.getIn().getHeader("ipAddress"),
                            calculateSignature(exchange),
                            timestamp,
                            transactionId
                    );
                    exchange.getIn().setBody(depositBody);
                })
                .setHeader("merchantId", constant(MERCHANT_ID))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("Deposit initiation request headers: ${headers}")
                .log("${exchangeProperty.mobileMoneyName} [Mbet] PayBill Init Deposit Request Body: ${body}")
                .to(API_BASE_URL + "/payment/deposit?bridgeEndpoint=true&httpMethod=POST")
                .log("${exchangeProperty.mobileMoneyName} [Mbet] PayBill Init Deposit Response: ${body}")
                //.log("Deposit initiation response: ${body}");
                //.unmarshal().json(JsonLibrary.Jackson)
                /*.choice()
                .when(simple("${body[status]} == 200"))
                .log("Deposit initiated successfully, proceeding with confirmation")
                .to("direct:confirmDeposit")
                .otherwise()
                .log("Deposit initiation failed")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"Deposit initiation failed\"}"))*/
                .end();


        // Deposit confirmation route
        from("direct:confirmPayBillDeposit")
                .process(exchange -> {
                    // Parse response from previous step as String
                    String responseBody = exchange.getIn().getBody(String.class);
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

                    // Safely extract transaction details
                    Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                    if (dataMap == null) {
                        exchange.setException(new Exception("No transaction data found"));
                        return;
                    }

                    String transactionId = (String) dataMap.get("transactionId");
                    if (transactionId == null) {
                        exchange.setException(new Exception("Transaction ID is missing"));
                        return;
                    }

                    // Prepare confirmation payload
                    String confirmBody = String.format(
                            "{\"accountId\":%s,\"message\":\"success\",\"modePaymentClient\":\"USSD\"," +
                                    "\"paymentMethodType\":\"APP\",\"signature\":\"%s\",\"status\":\"CONFIRMED\"," +
                                    "\"timeStamp\":\"%s\",\"transactionRef\":\"%s\"}",
                            exchange.getIn().getHeader("userId"),
                            exchange.getIn().getHeader("signature"),
                            exchange.getIn().getHeader("timestamp"),
                            transactionId
                    );

                    // Set up headers and body for confirmation request
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                    exchange.getIn().setHeader("merchantId", MERCHANT_ID);
                    exchange.getIn().setHeader(Exchange.HTTP_URI, API_BASE_URL + "/payment/deposit/" + transactionId);
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
                    exchange.getIn().setBody(confirmBody);
                })
                .to("http://dummy")
                .unmarshal().json(JsonLibrary.Jackson)
                .process(exchange -> {
                    // update both balance and ledger

                    System.out.println("Deposit confirmation response: " + exchange.getIn().getBody(Map.class));
                    Map<String, Object> responseMessage = exchange.getIn().getBody(Map.class);
                    System.out.println("Deposit confirmation response body: " + responseMessage.get("message"));
                    // String responseMessage = exchange.getIn().getBody(String.class);

                    System.out.println("Deposit Confirmation Response Body: " + exchange.getIn().getBody(String.class));

                  /*  ObjectMapper mapper = new ObjectMapper();
                    DepositConfirmationDTO message = mapper.readValue(responseMessage, DepositConfirmationDTO.class);
*/
                    // Process the response body as needed
                    //TODO: Subject to change
                    DepositProcessor depositProcessor = this.constructorBuilder.getDepositProcessor();
                    depositProcessor.confirmPayBillDeposit(responseMessage, exchange.getIn().getHeader("payBillPayment", PayBillPayment.class));

                })
                .log("Deposit confirmation response: ${body}");

        //Customer Registrations
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
                            generateUrlSafeBase64(exchange.getIn().getHeader("userName", String.class)),
                            exchange.getIn().getHeader("userName"),
                            generateFourDigits()

                    );
                    exchange.getIn().setBody(depositBody);
                })
                .log("Customer registration request headers: ${headers}")
                .log("Customer registration request body: ${body}")
                .to("http://dummy")
                .unmarshal().json(JsonLibrary.Jackson)
                .log("Customer registration response: ${body}")
                .process(UserValidationRoute::processData)
                .choice()
                .when(simple("${body[result][status]} == 200"))
                .log("Customer registered successfully, proceeding with deposit")
                .to("direct:prepareDeposit")
                .otherwise()
                .log("Customer registration failed")
                .setBody(simple("{\"status\": \"ERROR\", \"message\": \"Customer registration failed\"}"))
                .end();

        from("direct:prepareDeposit")
                .process(this::processJsonBody)
                .choice()
                .when(header("operatorName").isEqualTo("AirtelMoney-Tanzania"))
                .to(CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_PAY_BILL_URI)
                .when(header("operatorName").isEqualTo("Mpesa-Tanzania"))
                .to(CamelConfiguration.RABBIT_PRODUCER_MPESA_PAY_BILL_URI)
                .when(header("operatorName").isEqualTo("Mixx_by_yas-Tanzania"))
                .to(CamelConfiguration.RABBIT_PRODUCER_TIGOPESA_PAY_BILL_URI)
                .when(header("operatorName").isEqualTo("Halopesa-Tanzania"))
                .to(CamelConfiguration.RABBIT_PRODUCER_HALOPESA_PAY_BILL_URI)
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

    private void processJsonBody(Exchange exchange) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            PayBillPaymentDto paymentDto = exchange.getProperty("payBillPayment", PayBillPaymentDto.class);
            String jsonPayload = mapper.writeValueAsString(paymentDto);
            String mobileMoneyName = this.mnoService.searchMno(paymentDto.getMsisdn());
            exchange.getIn().setHeader("operatorName", mobileMoneyName);
            exchange.getIn().setBody(jsonPayload);
        } catch (JsonProcessingException e) {
            exchange.setException(new RuntimeException("Failed to convert PayBillPaymentDto to JSON", e));
        }
    }

    /*private Processor validateUserProcessor() {
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
            exchange.getIn().setHeader("userId", userData.get("userId"));

            System.out.println("userId: >>>>>>>>>>>>>>>>>>> " + userData.get("userId"));
        };
    }*/

    public Processor validateUserProcessor() {
        return exchange -> {
            // Retrieve the PayBillPayment object from the exchange
            System.out.println("payBillPaymentDto 1: >>>>>>>>>>>>>>>> ");
            PayBillPayment payBillPayment = exchange.getIn().getHeader("payBillPayment", PayBillPayment.class);


            Map<String, Object> response = exchange.getIn().getBody(Map.class);
            // if response is not null and status is 400, then it means the user is not found
            if (response != null && response.containsKey("status") && response.get("status").equals(400)) {
                System.out.println("PayBillPayment before error handling 1: " + payBillPayment);
                System.out.println("PayBillPayment before error handling 2: " + payBillPayment.getPaymentReference());
                String errorMessage = "Validation failed:" + response.get("message");
                updatePayBillPaymentWithError(payBillPayment, errorMessage);
                exchange.getIn().setBody(createErrorResponse(errorMessage));
            } else {
                // Check if response is null
                if (response == null) {
                    String errorMessage = "Validation failed: Empty response";
                    updatePayBillPaymentWithError(payBillPayment, errorMessage);
                    exchange.getIn().setBody(createErrorResponse(errorMessage));
                    return;
                }

                // Check if data array exists and is not empty
                Object dataObj = response.get("data");
                if (!(dataObj instanceof List) || ((List<?>) dataObj).isEmpty()) {
                    String errorMessage = "Validation failed: User not found";
                    updatePayBillPaymentWithError(payBillPayment, errorMessage);
                    exchange.getIn().setBody(createErrorResponse(errorMessage));
                    return;
                }

                // Get the first user from data array
                Map<String, Object> userData = (Map<String, Object>) ((List<?>) dataObj).get(0);

                // Check account status
                String accountStatus = (String) userData.get("accountStatus");
                if (accountStatus == null) {
                    String errorMessage = "Validation failed: Account status not found";
                    updatePayBillPaymentWithError(payBillPayment, errorMessage);
                    exchange.getIn().setBody(createErrorResponse(errorMessage));
                    return;
                }

                if (!"ACTIVE".equals(accountStatus)) {
                    String errorMessage = "Validation failed: Account is not active";
                    updatePayBillPaymentWithError(payBillPayment, errorMessage);
                    exchange.getIn().setBody(createErrorResponse(errorMessage));
                    return;
                }

                // If all validations pass, set the original successful response
                response.put("status", 200);
                response.put("message", "User validation successful");
                exchange.getIn().setBody(response);

                // Store user ID for later use
                exchange.setProperty("userId", userData.get("userId"));
                exchange.getIn().setHeader("userId", userData.get("userId"));

                System.out.println("userId: >>>>>>>>>>>>>>>>>>> " + userData.get("userId"));
            }


        };
    }

    private void updatePayBillPaymentWithError(PayBillPayment payBillPaymentDto, String errorMessage) {
        System.out.println("payBillPaymentDto 2: >>>>>>>>>>>>>>>> " + payBillPaymentDto.getPaymentReference());
        payBillPaymentService.findById(payBillPaymentDto.getId()).ifPresent(payBillPayment -> {
            payBillPayment.setCollectionStatus(CollectionStatus.REJECTED);
            payBillPayment.setErrorMessage(errorMessage);
            payBillPaymentService.update(payBillPayment);
        });

       /* if (payBillPayment != null) {
            // Update collection status to reflect the validation failure
            payBillPayment.setCollectionStatus(CollectionStatus.FAILED);
            payBillPayment.setErrorMessage(errorMessage);

            // You'll need to inject or obtain the appropriate repository to save the updated entity
            // For example:
            // payBillPaymentRepository.save(payBillPayment);
        }*/
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 400);
        errorResponse.put("message", message);
        return errorResponse;
    }

    public static String generateHmacSHA512(String plainText, String secretKey) {
        try {
            // Define the HMAC algorithm
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");

            // Create secret key specification
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );

            // Initialize the HMAC with the secret key
            sha512Hmac.init(secretKeySpec);

            // Generate the HMAC
            byte[] hmacBytes = sha512Hmac.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            // Convert to hexadecimal
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }

    // Helper method to convert byte array to hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Verification method
    public static boolean verifyHmac(String originalText, String secretKey, String receivedHmac) {
        String computedHmac = generateHmacSHA512(originalText, secretKey);
        return computedHmac.equals(receivedHmac);
    }

    public static String calculateSignature(Exchange exchange) {
        try {

            String userId = exchange.getIn().getHeader("userId", String.class);
            System.out.println("userId: " + userId);
            String timestamp = exchange.getProperty("timestamp", String.class);
            System.out.println("timestamp: " + timestamp);
            String message = userId + "+" + timestamp + "+" + MERCHANT_ID;
            //userId + timestamp + MERCHANT_ID + API_SECRET_KEY;
            // Generate HMAC-SHA512 hash

            //String message = "1236083+2025-01-23T08:33:37.287Z+2hMDwxphhOCjHjVDPWkFrPM5ugH3dsEhFwB";
            String secretKey = API_SECRET_KEY;

            // Generate HMAC
            String hmacSignature = generateHmacSHA512(message, secretKey);
            System.out.println("HMAC-SHA512 Signature: " + hmacSignature);

            // Verify HMAC
            boolean isValid = verifyHmac(message, secretKey, hmacSignature);
            System.out.println("HMAC Verification: " + isValid);

            exchange.setProperty("signature", hmacSignature);
            exchange.getIn().setHeader("signature", hmacSignature);

            return hmacSignature;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*private String calculateSignature(Exchange exchange) {
        try {
            String userId = exchange.getProperty("userId", String.class);
            System.out.println("userId: " + userId);
            String timestamp = exchange.getProperty("timestamp", String.class);
            System.out.println("timestamp: " + timestamp);

            // Concatenate the required fields
            String dataToSign = userId + timestamp + MERCHANT_ID + API_SECRET_KEY;

            // Create SHA-512 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(dataToSign.getBytes("UTF-8"));

            // Convert to hexadecimal
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }*/

    /*public static String calculateSignature(Exchange exchange) {
        String userId = exchange.getProperty("userId", String.class);
        String timestamp = exchange.getProperty("timestamp", String.class);
        String merchantId = exchange.getIn().getHeader("merchantId", String.class);
        String secretKey = exchange.getProperty("secretKey", String.class);

        try {
            String dataToHash = userId + timestamp + merchantId + secretKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not found", e);
        }
    }*/

    private String getCurrentTimestamp() {
        return DateTimeFormatter
                .ISO_INSTANT
                .format(Instant.now().atOffset(ZoneOffset.UTC));
    }

    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }


    private void process(Exchange exchange) {
        ObjectMapper mapper = new ObjectMapper();
        PayBillPaymentDto payBillPaymentDto = null;
        try {
            payBillPaymentDto = mapper.readValue(exchange.getIn().getBody(String.class), PayBillPaymentDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        exchange.setProperty("payBillPayment", payBillPaymentDto);
        exchange.setProperty("userName", payBillPaymentDto.getMsisdn().substring(3));
        exchange.setProperty("accountId", "1000002");
        exchange.setProperty("amount", String.valueOf(payBillPaymentDto.getAmount()).replaceAll("\\.\\d+", "") + "00");
        exchange.setProperty("ipAddress", "192.2.2.22");

        Map<String, Object> headers = new HashMap<>();
        headers.put("userName", payBillPaymentDto.getMsisdn().substring(3));
        headers.put("accountId", "1000002");
        headers.put("amount", String.valueOf(payBillPaymentDto.getAmount()).replaceAll("\\.\\d+", "") + "00");
        headers.put("ipAddress", "192.2.2.22");
        headers.put("payBillPayment", payBillPaymentDto);

        exchange.getIn().setHeaders(headers);


    }
}
