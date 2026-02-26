package com.hybrid9.pg.Lipanasi.services.payments.crdb.cybersorce;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.BillingInfo;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.CaptureContextRequest;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentResponse;
import com.hybrid9.pg.Lipanasi.resources.excpts.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.TimeZone;

@Service
public class CyberSourceMicroformService {

    private static final Logger logger = LoggerFactory.getLogger(CyberSourceMicroformService.class);

    @Value("${cybersource.merchant.id}")
    private String merchantId;

    @Value("${cybersource.api.key.id}")
    private String apiKeyId;

    @Value("${cybersource.secret.key}")
    private String secretKey;

    @Value("${cybersource.environment:production}")
    private String environment;

    private static final String CAPTURE_CONTEXT_ENDPOINT = "/microform/v2/sessions";
    private static final String PAYMENT_ENDPOINT = "/pts/v2/payments";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Generate Capture Context for Microform
     */
    public String generateCaptureContext(CaptureContextRequest request) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("clientVersion", "v2");

            // Target origins for security
            List<String> targetOrigins = Arrays.asList(request.getTargetOrigins());
            requestBody.put("targetOrigins", targetOrigins);

            // Allowed card networks
            requestBody.put("allowedCardNetworks", Arrays.asList("VISA"));

            // Payment types
            requestBody.put("allowedPaymentTypes", Arrays.asList("CARD"));

            String requestJson = new Gson().toJson(requestBody);

            // Make API call using HTTP signature authentication
            HttpHeaders headers = createAuthHeaders("POST", CAPTURE_CONTEXT_ENDPOINT, requestJson);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            logger.info("Making API call to {}", CAPTURE_CONTEXT_ENDPOINT);
            logger.info("Request Body: {}", requestBody);

            String baseUrl = getBaseUrl();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + CAPTURE_CONTEXT_ENDPOINT,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Check if response is successful
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = response.getBody().trim();

                // Remove quotes if the response is a quoted string
                if (responseBody.startsWith("\"") && responseBody.endsWith("\"")) {
                    responseBody = responseBody.substring(1, responseBody.length() - 1);
                }

                // The response should be a JWT string (capture context)
                if (responseBody.length() > 0 && responseBody.contains(".")) {
                    logger.info("Capture context generated successfully for merchant: {}", merchantId);
                    return responseBody;
                } else {
                    logger.error("Invalid capture context format received: {}", responseBody);
                    throw new PaymentException("Invalid capture context format received");
                }
            } else {
                logger.error("Failed to generate capture context. Status: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
                throw new PaymentException("Failed to generate capture context");
            }

        } catch (Exception e) {
            logger.error("Error generating capture context", e);
            throw new PaymentException("Error generating capture context: " + e.getMessage());
        }
    }

    /**
     * Process payment using tokenized card data from Microform
     */
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        try {
            Map<String, Object> requestBody = buildPaymentRequest(paymentRequest);
            String requestJson = new Gson().toJson(requestBody);

            logger.info("Processing payment for amount: {} {}",
                    paymentRequest.getAmount(), paymentRequest.getCurrency());

            // Make API call using HTTP signature authentication
            HttpHeaders headers = createAuthHeaders("POST", PAYMENT_ENDPOINT, requestJson);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            String baseUrl = getBaseUrl();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + PAYMENT_ENDPOINT,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return parsePaymentResponse(response.getBody());

        } catch (Exception e) {
            logger.error("Error processing payment", e);
            throw new PaymentException("Payment processing failed: " + e.getMessage());
        }
    }

    private HttpHeaders createAuthHeaders(String method, String endpoint, String requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Use GMT timezone and proper format
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String timestamp = dateFormat.format(new Date());

            String digest = createDigest(requestBody);
            String host = getHost();

            // Build signature string in exact order required by CyberSource
            String signatureString = buildSignatureString(method, endpoint, host, timestamp, digest);
            String signature = createSignature(signatureString);

            headers.set("v-c-merchant-id", merchantId);
            headers.set("Date", timestamp);
            headers.set("Host", host);
            headers.set("Digest", "SHA-256=" + digest);
            headers.set("Signature", String.format(
                    "keyid=\"%s\", algorithm=\"HmacSHA256\", headers=\"host date (request-target) digest v-c-merchant-id\", signature=\"%s\"",
                    apiKeyId, signature
            ));

            return headers;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create authentication headers", e);
        }
    }

    private String createDigest(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create digest", e);
        }
    }

    private String buildSignatureString(String method, String endpoint, String host, String timestamp, String digest) {
        return String.format("host: %s\ndate: %s\n(request-target): %s %s\ndigest: SHA-256=%s\nv-c-merchant-id: %s",
                host, timestamp, method.toLowerCase(), endpoint, digest, merchantId);
    }

    private String createSignature(String signatureString) {
        try {
            byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(signatureString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create signature", e);
        }
    }

    private String getBaseUrl() {
        if ("sandbox".equalsIgnoreCase(environment) || "test".equalsIgnoreCase(environment)) {
            return "https://apitest.cybersource.com";
        } else {
            return "https://api.cybersource.com";
        }
    }

    private String getHost() {
        if ("sandbox".equalsIgnoreCase(environment) || "test".equalsIgnoreCase(environment)) {
            return "apitest.cybersource.com";
        } else {
            return "api.cybersource.com";
        }
    }

    /*private Map<String, Object> buildPaymentRequest(PaymentRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // Client reference information
        Map<String, Object> clientReferenceInformation = new HashMap<>();
        clientReferenceInformation.put("code", request.getOrderId());
        requestBody.put("clientReferenceInformation", clientReferenceInformation);

        // Payment information
        Map<String, Object> paymentInformation = new HashMap<>();
        Map<String, Object> card = new HashMap<>();
        card.put("securityCode", request.getCvv());

        // Use tokenized card data from Microform
        Map<String, Object> tokenizedCard = new HashMap<>();
        tokenizedCard.put("transientTokenJwt", request.getTransientToken());
        //card.put("tokenizedCard", tokenizedCard);

        //paymentInformation.put("card", card);
        requestBody.put("tokenInformation", tokenizedCard);

        // Order information
        Map<String, Object> orderInformation = new HashMap<>();
        Map<String, Object> amountDetails = new HashMap<>();
        amountDetails.put("totalAmount", request.getAmount());
        amountDetails.put("currency", request.getCurrency());
        orderInformation.put("amountDetails", amountDetails);

        // Billing information
        if (request.getBillingInfo() != null) {
            Map<String, Object> billTo = new HashMap<>();
            BillingInfo billing = request.getBillingInfo();
            billTo.put("firstName", billing.getFirstName());
            billTo.put("lastName", billing.getLastName());
            billTo.put("address1", billing.getAddress1());
            billTo.put("locality", billing.getCity());
            billTo.put("administrativeArea", billing.getState());
            billTo.put("postalCode", billing.getZipCode());
            billTo.put("country", billing.getCountry());
            billTo.put("email", billing.getEmail());
            billTo.put("phoneNumber", billing.getPhone());
            orderInformation.put("billTo", billTo);
        }

        requestBody.put("orderInformation", orderInformation);

        // Processing information for production
        Map<String, Object> processingInformation = new HashMap<>();
        processingInformation.put("capture", request.isCapture());
        processingInformation.put("commerceIndicator", "internet");
        //requestBody.put("processingInformation", processingInformation);

        logger.info("Payment Request Body: " + requestBody);

        return requestBody;
    }*/

    private Map<String, Object> buildPaymentRequest(PaymentRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // Client reference information
        Map<String, Object> clientReferenceInformation = new HashMap<>();
        clientReferenceInformation.put("code", request.getOrderId());
        requestBody.put("clientReferenceInformation", clientReferenceInformation);

        // Token information for tokenized card data from Microform
        Map<String, Object> tokenizedCard = new HashMap<>();
        tokenizedCard.put("transientTokenJwt", request.getTransientToken());
        requestBody.put("tokenInformation", tokenizedCard);

        // Order information
        Map<String, Object> orderInformation = new HashMap<>();
        Map<String, Object> amountDetails = new HashMap<>();
        amountDetails.put("totalAmount", String.valueOf(request.getAmount()));
        amountDetails.put("currency", request.getCurrency());
        orderInformation.put("amountDetails", amountDetails);

        // Billing information with proper validation
        if (request.getBillingInfo() != null) {
            Map<String, Object> billTo = new HashMap<>();
            BillingInfo billing = request.getBillingInfo();

            // Only add non-null and non-"null" string values
            addIfNotNullOrEmpty(billTo, "firstName", billing.getFirstName());
            addIfNotNullOrEmpty(billTo, "lastName", billing.getLastName());
            addIfNotNullOrEmpty(billTo, "address1", billing.getAddress1());
            addIfNotNullOrEmpty(billTo, "locality", billing.getCity());
            addIfNotNullOrEmpty(billTo, "administrativeArea", billing.getState());
            addIfNotNullOrEmpty(billTo, "postalCode", billing.getZipCode());
            addIfNotNullOrEmpty(billTo, "email", billing.getEmail());
            addIfNotNullOrEmpty(billTo, "phoneNumber", billing.getPhone());

            // Special handling for country - must be ISO 3166-1 alpha-2 code
            String country = billing.getCountry();
            if (country != null && !country.equals("null") && !country.trim().isEmpty()) {
                // Validate and convert country to proper ISO code
                String validCountry = validateAndGetCountryCode(country);
                if (validCountry != null) {
                    billTo.put("country", validCountry);
                }
            }

            orderInformation.put("billTo", billTo);
        }

        requestBody.put("orderInformation", orderInformation);

        // Processing information
        Map<String, Object> processingInformation = new HashMap<>();
        processingInformation.put("capture", request.isCapture());
        processingInformation.put("commerceIndicator", "internet");
        requestBody.put("processingInformation", processingInformation);

        logger.info("Payment Request Body: " + requestBody);

        return requestBody;
    }

    // Helper method to add fields only if they're not null or "null" string
    private void addIfNotNullOrEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.equals("null") && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    // Helper method to validate and get proper country code
    private String validateAndGetCountryCode(String country) {
        if (country == null || country.equals("null") || country.trim().isEmpty()) {
            return null;
        }

        // If it's already a 2-letter code, validate it's uppercase
        if (country.length() == 2) {
            return country.toUpperCase();
        }

        // Map common country names to ISO codes (add more as needed)
        Map<String, String> countryMap = new HashMap<>();
        countryMap.put("TANZANIA", "TZ");
        countryMap.put("UNITED STATES", "US");
        countryMap.put("KENYA", "KE");
        countryMap.put("UGANDA", "UG");
        // Add more countries as needed

        String upperCountry = country.toUpperCase();
        return countryMap.getOrDefault(upperCountry, null);
    }

    private PaymentResponse parsePaymentResponse(String response) {
        JsonObject responseObj = new Gson().fromJson(response, JsonObject.class);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setTransactionId(responseObj.get("id").getAsString());
        paymentResponse.setStatus(responseObj.get("status").getAsString());

        if (responseObj.has("processorInformation")) {
            JsonObject processorInfo = responseObj.getAsJsonObject("processorInformation");
            if (processorInfo.has("approvalCode")) {
                paymentResponse.setApprovalCode(processorInfo.get("approvalCode").getAsString());
            }
            if (processorInfo.has("responseCode")) {
                paymentResponse.setResponseCode(processorInfo.get("responseCode").getAsString());
            }
        }

        if (responseObj.has("orderInformation")) {
            JsonObject orderInfo = responseObj.getAsJsonObject("orderInformation");
            if (orderInfo.has("amountDetails")) {
                JsonObject amountDetails = orderInfo.getAsJsonObject("amountDetails");
                paymentResponse.setAmount(amountDetails.get("authorizedAmount").getAsString());
                paymentResponse.setCurrency(amountDetails.get("currency").getAsString());
            }
        }

        paymentResponse.setSuccess("AUTHORIZED".equals(paymentResponse.getStatus()));

        return paymentResponse;
    }
}