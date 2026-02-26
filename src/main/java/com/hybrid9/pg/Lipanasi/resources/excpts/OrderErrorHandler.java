package com.hybrid9.pg.Lipanasi.resources.excpts;

import com.hybrid9.pg.Lipanasi.dto.order.OrderResponse;
import com.hybrid9.pg.Lipanasi.enums.ListOfErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class OrderErrorHandler {
    public ResponseEntity<Object> handleError(Throwable cause) {
        ListOfErrorCode errorCode = determineErrorCode(cause);

        // Log the error with appropriate level
        logError(cause, errorCode);

        Object errorResponse = createErrorResponse(errorCode, cause);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }

    public ResponseEntity<Object> handleRateLimitError(CustomExcpts.RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());

        OrderResponse errorResponse = new OrderResponse();
        errorResponse.setSuccessful(false);
        errorResponse.setErrorCode(ListOfErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        errorResponse.setMessage(buildRateLimitMessage(e));
        errorResponse.setStatus("FAILED");

        log.info("Rate limit exceeded: {}", "One");

        return ResponseEntity.status(ListOfErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus())
                .header("Retry-After", String.valueOf(e.getTimeWindowSeconds()))
                .header("X-RateLimit-Limit", String.valueOf(e.getMaxRequests()))
                .header("X-RateLimit-Window", String.valueOf(e.getTimeWindowSeconds()))
                .header("X-RateLimit-Type", e.getRateLimitType())
                .body(errorResponse);
    }

    private ListOfErrorCode determineErrorCode(Throwable cause) {
        return switch (cause.getClass().getSimpleName()) {
            case "RateLimitExceededException" -> ListOfErrorCode.RATE_LIMIT_EXCEEDED;
            case "UnauthorizedException" -> ListOfErrorCode.UNAUTHORIZED;
            case "OrderNotFoundException" -> ListOfErrorCode.ORDER_NOT_FOUND;
            case "OperatorNotFoundException" -> ListOfErrorCode.OPERATOR_NOT_FOUND;
            case "VendorNotFoundException" -> ListOfErrorCode.VENDOR_NOT_FOUND;
            case "InvalidVendorInfoException" -> ListOfErrorCode.INVALID_VENDOR_INFO;
            case "InvalidPaymentMethodException" -> ListOfErrorCode.INVALID_PAYMENT_METHOD;
            case "InvalidSessionException" -> ListOfErrorCode.SESSION_EXPIRED;
            case "PaymentMethodNotFoundException" -> ListOfErrorCode.PAYMENT_METHOD_NOT_FOUND;
            case "PaymentGatewayException" -> ListOfErrorCode.PAYMENT_FAILED;
            case "TransactionNotFoundException" -> ListOfErrorCode.TRANSACTION_NOT_FOUND;
            case "PhoneNumberException" -> ListOfErrorCode.INVALID_PHONE_NUMBER;
            case "PaymentException" -> ListOfErrorCode.PAYMENT_PROCESSING_ERROR;
            case "IllegalArgumentException" -> ListOfErrorCode.INVALID_REQUEST;
            case "TimeoutException" -> ListOfErrorCode.PAYMENT_TIMEOUT;
            case "PartnerValidationException" -> ListOfErrorCode.PARTNER_VALIDATION_FAILED;
            case "ConnectException", "SocketTimeoutException" -> ListOfErrorCode.NETWORK_ERROR;
            case "JsonProcessingException", "UnrecognizedPropertyException" -> ListOfErrorCode.CONFIGURATION_ERROR;
            default -> {
                // Check by instance for cases where switch doesn't work
                if (cause instanceof TimeoutException) {
                    yield ListOfErrorCode.PAYMENT_TIMEOUT;
                } else if (cause instanceof ConnectException || cause instanceof SocketTimeoutException) {
                    yield ListOfErrorCode.NETWORK_ERROR;
                } else {
                    yield ListOfErrorCode.INTERNAL_SERVER_ERROR;
                }
            }
        };
    }

    private Object createErrorResponse(ListOfErrorCode errorCode, Throwable cause) {
        OrderResponse errorResponse = new OrderResponse();
        errorResponse.setSuccessful(false);
        errorResponse.setErrorCode(errorCode.getCode());
        errorResponse.setStatus(errorCode.getCode().startsWith("00") ? "SUCCESS" : "FAILED");

        // Use custom message if available, otherwise use default
        String message = (cause.getMessage() != null && !cause.getMessage().isEmpty())
                ? "Order creation failed"
                : errorCode.getMessage();

        errorResponse.setMessage(message);
       // errorResponse.setMessage(errorCode.getMessage() != null ? !cause.getMessage().isEmpty() ? errorCode.getMessage() : "Order creation failed" : "Order creation failed");


        return errorResponse;
    }

    private void logError(Throwable cause, ListOfErrorCode errorCode) {
        switch (errorCode.getHttpStatus().series()) {
            case CLIENT_ERROR -> log.warn("Client error [{}]: {}", errorCode.getCode(), cause.getMessage());
            case SERVER_ERROR -> log.error("Server error [{}]: {}", errorCode.getCode(), cause.getMessage(), cause);
            default -> log.info("Payment error [{}]: {}", errorCode.getCode(), cause.getMessage());
        }
    }

    private String buildRateLimitMessage(CustomExcpts.RateLimitExceededException e) {
        return String.format("Rate limit exceeded. Maximum %d requests per %d seconds allowed. Please try again later.",
                e.getMaxRequests(), e.getTimeWindowSeconds());
    }
}
