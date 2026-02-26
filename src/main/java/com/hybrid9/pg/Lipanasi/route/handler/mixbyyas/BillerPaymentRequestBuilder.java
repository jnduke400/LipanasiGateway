package com.hybrid9.pg.Lipanasi.route.handler.mixbyyas;

import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.BillerPaymentRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
public class BillerPaymentRequestBuilder {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Creates a biller payment request from the exchange properties
     */
    public BillerPaymentRequest createBillerPaymentRequest(Exchange exchange) {
        try {
            // Get input data from exchange properties
            PaymentRequestInput input = exchange.getProperty("paymentInput", PaymentRequestInput.class);
            String billerCode = exchange.getProperty("billerCode", String.class);

            // Create and populate the request
            BillerPaymentRequest request = new BillerPaymentRequest();

            // Set mandatory fields
            request.setCustomerMSISDN(formatMsisdn(input.getCustomerMsisdn()));
            request.setBillerMSISDN(input.getBillerMsisdn());
            request.setAmount(input.getAmount());
            /*request.setReferenceID(generateReferenceId(billerCode));*/
            request.setReferenceID(input.getReferenceId());

            // Set optional fields if present
            if (input.getRemarks() != null && !input.getRemarks().trim().isEmpty()) {
                request.setRemarks(sanitizeRemarks(input.getRemarks()));
            }

            // Log the created request
            log.debug("Created biller payment request: {}", request);

            return request;
        } catch (Exception e) {
            log.error("Error creating biller payment request: {}", e.getMessage());
            throw new RequestCreationException("Failed to create biller payment request", e);
        }
    }

    /**
     * Custom exception for request creation errors
     */
    public class RequestCreationException extends RuntimeException {
        public RequestCreationException(String message) {
            super(message);
        }

        public RequestCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Request input data class
     */
    @Data
    public static class PaymentRequestInput {
        private String customerMsisdn;
        private String billerMsisdn;
        private String referenceId;
        private BigDecimal amount;
        private String remarks;
    }

    /**
     * Formats MSISDN to ensure it starts with 255
     */
    private String formatMsisdn(String msisdn) {
        if (msisdn == null || msisdn.trim().isEmpty()) {
            throw new IllegalArgumentException("MSISDN cannot be null or empty");
        }

        // Remove any spaces or special characters
        msisdn = msisdn.replaceAll("[^0-9]", "");

        // If number starts with 0, replace with 255
        if (msisdn.startsWith("0")) {
            msisdn = "255" + msisdn.substring(1);
        }

        // If number starts with +255, remove the +
        if (msisdn.startsWith("+255")) {
            msisdn = msisdn.substring(1);
        }

        // Validate final format
        if (!msisdn.matches("^255\\d{9}$")) {
            throw new IllegalArgumentException("Invalid MSISDN format: " + msisdn);
        }

        return msisdn;
    }

    /**
     * Generates a unique reference ID using biller code and timestamp
     */
    private String generateReferenceId(String billerCode) {
        if (billerCode == null || billerCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Biller code cannot be null or empty");
        }

        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s%s%s", billerCode, timestamp, uniqueId);
    }

    /**
     * Sanitizes remarks to prevent injection and ensure proper formatting
     */
    private String sanitizeRemarks(String remarks) {
        if (remarks == null) {
            return null;
        }

        // Remove any potentially harmful characters
        remarks = remarks.replaceAll("[<>\"'%;()&+]", "");

        // Trim and limit length
        remarks = remarks.trim();
        if (remarks.length() > 255) {
            remarks = remarks.substring(0, 255);
        }

        return remarks;
    }
}
