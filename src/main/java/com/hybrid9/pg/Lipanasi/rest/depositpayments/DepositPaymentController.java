package com.hybrid9.pg.Lipanasi.rest.depositpayments;

import com.hybrid9.pg.Lipanasi.annotationx.RateLimit;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositRequest;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositResponse;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentResponse;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/deposits")
@Slf4j
public class DepositPaymentController {
    private final SessionManagementService sessionManagementService;
    private final PushUssdService pushUssdService;
    private final VendorManagementService vendorManagementService;
    private final RestTemplate restTemplate;

    public DepositPaymentController(SessionManagementService sessionManagementService, PushUssdService pushUssdService, VendorManagementService vendorManagementService, RestTemplate restTemplate) {
        this.sessionManagementService = sessionManagementService;
        this.pushUssdService = pushUssdService;
        this.vendorManagementService = vendorManagementService;
        this.restTemplate = restTemplate;
    }

    @Value("${app.security.api-key:default-api-key}")
    private String validApiKey;

    @RateLimit(maxRequests = 100, timeWindow = 60, type = RateLimit.RateLimitType.API)
    @PostMapping(value = "/payment", consumes = "application/json", produces = "application/json")
    public CompletableFuture<ResponseEntity<DepositResponse>> depositPayment(
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestBody DepositRequest depositRequest) {

        try {
            // Step 1: Verify API Key
            if (!isValidApiKey(apiKey)) {
                log.warn("Invalid or missing API key provided");
                DepositResponse errorResponse = DepositResponse.builder()
                        .status("FAILED")
                        .message("Invalid or missing API key")
                        .reference(depositRequest.getReference())
                        .build();
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
                );
            }

            // Step 2: Get vendor details (from session or database)
            VendorInfo vendorInfo = getVendorDetails(depositRequest);

            if (vendorInfo == null || !StringUtils.hasText(vendorInfo.getVendorExternalId())) {
                log.error("Unable to retrieve vendor details for session: {} or reference: {}",
                        depositRequest.getPaymentSessionId(), depositRequest.getReference());
                DepositResponse errorResponse = DepositResponse.builder()
                        .status("FAILED")
                        .message("Vendor configuration not found")
                        .reference(depositRequest.getReference())
                        .build();
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
                );
            }

            log.info("Processing deposit payment for vendor: {} with external ID: {}",
                    vendorInfo.getVendorId(), vendorInfo.getVendorExternalId());

            // Step 3: Forward request to vendor-specific endpoint
            //TODO: Uncomment this line when the vendor endpoint is ready
            //DepositResponse response = forwardToVendorEndpoint(depositRequest, vendorInfo);
            DepositResponse response = DepositResponse.builder()
                    .status("SUCCESS")
                    .message("Successfully processed deposit payment")
                    .reference(depositRequest.getReference())
                    .build();

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));

        } catch (Exception e) {
            log.error("Error processing deposit payment for reference: {}",
                    depositRequest.getReference(), e);

            DepositResponse errorResponse = DepositResponse.builder()
                    .status("FAILED")
                    .message("Internal server error occurred")
                    .reference(depositRequest.getReference())
                    .build();

            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
            );
        }
    }

    /**
     * Validates the provided API key
     */
    private boolean isValidApiKey(String apiKey) {
        return StringUtils.hasText(apiKey) && validApiKey.equals(apiKey);
    }

    /**
     * Retrieves vendor details from session (if active) or database (if session expired)
     */
    private VendorInfo getVendorDetails(DepositRequest depositRequest) {
        VendorInfo vendorInfo = null;

        // First, try to get vendor details from active session
        if (StringUtils.hasText(depositRequest.getPaymentSessionId())) {
            vendorInfo = getVendorFromSession(depositRequest.getPaymentSessionId());

            if (vendorInfo != null) {
                log.debug("Retrieved vendor details from active session: {}", depositRequest.getPaymentSessionId());
                return vendorInfo;
            }
        }

        // If session is expired or not found, get vendor details from database
        if (StringUtils.hasText(depositRequest.getReference())) {
            vendorInfo = getVendorFromDatabase(depositRequest.getReference());

            if (vendorInfo != null) {
                log.debug("Retrieved vendor details from database for reference: {}", depositRequest.getReference());
            }
        }

        return vendorInfo;
    }

    /**
     * Retrieves vendor details from active session
     */
    private VendorInfo getVendorFromSession(String sessionId) {
        try {
            Optional<UserSession> sessionOpt = sessionManagementService.getSession(sessionId);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                String merchantId = session.getMerchantId();

                if (StringUtils.hasText(merchantId)) {
                    Optional<VendorManager> vendorOpt = vendorManagementService.getVendor(merchantId);

                    if (vendorOpt.isPresent()) {
                        VendorManager vendor = vendorOpt.get();
                        return VendorInfo.builder()
                                .vendorId(vendor.getVendorId())
                                .vendorExternalId(vendor.getVendorExternalId())
                                .vendorCallbackUrl(vendor.getVendorCallbackUrl())
                                .vendorName(vendor.getVendorName())
                                .source("SESSION")
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving vendor details from session: {}", sessionId, e);
        }

        return null;
    }

    /**
     * Retrieves vendor details from database using PushUssd entity
     */
    private VendorInfo getVendorFromDatabase(String reference) {
        try {
            PushUssd pushUssd = pushUssdService.findByReference(reference);

            if (pushUssd != null && pushUssd.getVendorDetails() != null) {
                // Assuming VendorDetails has the necessary fields
                return VendorInfo.builder()
                        .vendorId(pushUssd.getVendorDetails().getId().toString())
                        .vendorExternalId(pushUssd.getVendorDetails().getVendorExternalId())
                        .vendorCallbackUrl(pushUssd.getVendorDetails().getCallbackUrl())
                        .vendorName(pushUssd.getVendorDetails().getVendorName())
                        .source("DATABASE")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving vendor details from database for reference: {}", reference, e);
        }

        return null;
    }

    /**
     * Forwards the request to vendor-specific endpoint based on vendor configuration
     */
    private DepositResponse forwardToVendorEndpoint(DepositRequest depositRequest, VendorInfo vendorInfo) {
        try {
            String vendorEndpoint = buildVendorEndpoint(vendorInfo);

            if (!StringUtils.hasText(vendorEndpoint)) {
                log.error("Invalid vendor callback URL for vendor: {}", vendorInfo.getVendorExternalId());
                return DepositResponse.builder()
                        .status("FAILED")
                        .message("Vendor endpoint configuration error")
                        .reference(depositRequest.getReference())
                        .build();
            }

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Vendor-ID", vendorInfo.getVendorExternalId());
            headers.set("X-Source-System", "SCOOP-PG");

            // Create HTTP entity with request body and headers
            HttpEntity<DepositRequest> requestEntity = new HttpEntity<>(depositRequest, headers);

            log.info("Forwarding deposit request to vendor endpoint: {} for vendor: {}",
                    vendorEndpoint, vendorInfo.getVendorExternalId());

            // Make HTTP call to vendor endpoint
            ResponseEntity<DepositResponse> response = restTemplate.exchange(
                    vendorEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    DepositResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully processed deposit payment with vendor: {} for reference: {}",
                        vendorInfo.getVendorExternalId(), depositRequest.getReference());
                return response.getBody();
            } else {
                log.error("Vendor endpoint returned error status: {} for vendor: {}",
                        response.getStatusCode(), vendorInfo.getVendorExternalId());
                return DepositResponse.builder()
                        .status("FAILED")
                        .message("Vendor processing error")
                        .reference(depositRequest.getReference())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error forwarding request to vendor endpoint for vendor: {}",
                    vendorInfo.getVendorExternalId(), e);
            return DepositResponse.builder()
                    .status("FAILED")
                    .message("Error communicating with vendor system")
                    .reference(depositRequest.getReference())
                    .build();
        }
    }

    /**
     * Builds the complete vendor endpoint URL
     */
    private String buildVendorEndpoint(VendorInfo vendorInfo) {
        String callbackUrl = vendorInfo.getVendorCallbackUrl();

        if (!StringUtils.hasText(callbackUrl)) {
            return null;
        }

        // Ensure URL ends with proper endpoint path
        if (!callbackUrl.endsWith("/")) {
            callbackUrl += "/";
        }

        // Append deposit payment endpoint path
        return callbackUrl + "api/deposits/payment";
    }

    /**
     * Inner class to hold vendor information
     */
    @lombok.Data
    @lombok.Builder
    private static class VendorInfo {
        private String vendorId;
        private String vendorExternalId;
        private String vendorCallbackUrl;
        private String vendorName;
        private String source; // SESSION or DATABASE
    }
}