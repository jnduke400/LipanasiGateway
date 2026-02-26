package com.hybrid9.pg.Lipanasi.rest.gwpayment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.annotationx.RateLimit;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentResponse;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.VerificationRequest;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.VerificationResponse;
import com.hybrid9.pg.Lipanasi.enums.ListOfErrorCode;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.resources.excpts.PaymentErrorHandler;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.PaymentProcessingService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

//@RestController
//@RequestMapping("/api/v1/payments")
@AllArgsConstructor
public class PaymentController {
    private final SessionManagementService sessionService;
    private final PaymentProcessingService paymentService;
    private final PaymentErrorHandler paymentErrorHandler;
    private final OrderService orderService;

    @RateLimit(maxRequests = 100, timeWindow = 60, type = RateLimit.RateLimitType.IP)
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<Object>> processPayment(@RequestBody PaymentRequest request,
                                                                    @RequestHeader("Session-ID") String sessionId,
                                                                    HttpServletRequest httpRequest) {
        try {
            // Validate session
            UserSession session = sessionService.getSession(sessionId)
                    .orElseThrow(() -> new CustomExcpts.UnauthorizedException("Invalid session"));

            // Validate order
            if(!session.getOrderNumber().equalsIgnoreCase(request.getOrderNumber())){
                throw new CustomExcpts.OrderNotFoundException("Order not found");
            }


            // Process payment
            return paymentService.processPayment(request, session, httpRequest)
                    .thenApply(result -> {
                        String json = new Gson().toJson(result);
                        JsonNode jsonNode = null;
                        try {
                            jsonNode = new ObjectMapper().readTree(json);

                            // Check if required fields exist before accessing them
                            JsonNode transactionIdNode = jsonNode.get("transactionId");
                            JsonNode successfulNode = jsonNode.get("successful");

                            if (transactionIdNode != null && !transactionIdNode.isNull() &&
                                    successfulNode != null && !successfulNode.isNull()) {

                                // Update session with transaction information only if fields exist
                                sessionService.updateTransactionState(
                                        sessionId,
                                        transactionIdNode.asText(),
                                        successfulNode.asBoolean() ?
                                                UserSession.TransactionStatus.INITIATED :
                                                UserSession.TransactionStatus.FAILED
                                );
                            }

                            return ResponseEntity.ok(result);

                        } catch (JsonProcessingException e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            return paymentErrorHandler.handleError(cause);
                        }
                    })
                    .exceptionally(e -> {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        return paymentErrorHandler.handleError(cause);
                    });

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    paymentErrorHandler.handleError(e)
            );
        }
    }

    @RateLimit(maxRequests = 5, timeWindow = 60, type = RateLimit.RateLimitType.IP)
    @PostMapping("/3ds-verify")
    public ResponseEntity<?> verify3dsChallenge(@RequestBody VerificationRequest request,
                                                @RequestHeader("Session-ID") String sessionId) {
        UserSession session = sessionService.getSession(sessionId)
                .orElseThrow(() -> new CustomExcpts.UnauthorizedException("Invalid session"));

        // Verify 3DS challenge
        VerificationResponse result = paymentService.verify3dsChallenge(request, session);

        // Update MFA verification status
        session.setMfaVerified(result.isVerified());
        sessionService.updateSession(sessionId, session);

        return ResponseEntity.ok(result);
    }

    @RateLimit(maxRequests = 30, timeWindow = 60, type = RateLimit.RateLimitType.IP)
    @GetMapping(value = "/status/{orderNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getPaymentStatus(@RequestHeader("Session-ID") String sessionId,
                                                            @PathVariable("orderNumber") String orderNumber) {
        try {
            // Validate session
            UserSession session = sessionService.getSession(sessionId)
                    .orElseThrow(() -> new CustomExcpts.UnauthorizedException("Invalid session"));

            // Validate order
            if(!session.getOrderNumber().equalsIgnoreCase(orderNumber)){
                throw new CustomExcpts.OrderNotFoundException("Order not found");
            }

            Map<String, Object> response = new HashMap<>();
            String transactionStatus = session.getTransactionStatus().name();

            switch (transactionStatus.toLowerCase()) {
                case "completed" -> {
                    response.put("status", "SUCCESS");
                    response.put("message", "Payment completed successfully");
                    response.put("transactionId", session.getCurrentTransactionId());
                    response.put("successful", true);
                    response.put("errorCode", ListOfErrorCode.PAYMENT_SUCCESS.getCode());
                }
                case "initiated" -> {
                    response.put("status", "INITIATED");
                    response.put("message", "Payment initiated successfully");
                    response.put("transactionId", session.getCurrentTransactionId());
                    response.put("successful", true);
                    response.put("errorCode", ListOfErrorCode.PAYMENT_INITIATED.getCode());
                }
                case "pending" -> {
                    response.put("status", "PENDING");
                    response.put("message", "Payment pending");
                    response.put("transactionId", session.getCurrentTransactionId());
                    response.put("successful", false);
                    response.put("errorCode", ListOfErrorCode.PAYMENT_PENDING.getCode());
                }
                default -> {
                    response.put("status", "FAILED");
                    response.put("message", "Payment failed");
                    response.put("successful", false);
                    response.put("errorCode", ListOfErrorCode.PAYMENT_FAILED.getCode());
                }
            }


            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return paymentErrorHandler.handleError(e);

        }

    }
}
