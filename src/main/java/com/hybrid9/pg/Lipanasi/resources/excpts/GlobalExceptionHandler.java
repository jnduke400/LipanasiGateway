/*
package com.gtl.pg.scoop.resources.excpts;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {
    private final PaymentErrorHandler paymentErrorHandler;
    private final OrderErrorHandler orderErrorHandler;

    */
/**
     * Handles rate limit exceeded exceptions globally
     *//*

    @ExceptionHandler(CustomExcpts.RateLimitExceededException.class)
    public ResponseEntity<Object> handleRateLimitExceeded(CustomExcpts.RateLimitExceededException e) {
        log.warn("Global rate limit handler - Rate limit exceeded: {}", e.getMessage());
        return paymentErrorHandler.handleRateLimitError(e);
    }

    */
/**
     * Handles other order-related exceptions
     *//*

    @ExceptionHandler({
            CustomExcpts.OrderNotFoundException.class,
            CustomExcpts.PhoneNumberException.class,
            CustomExcpts.DatabaseOperationsException.class,
            CustomExcpts.TransactionNotFoundException.class,
            CustomExcpts.OperatorNotFoundException.class,
            CustomExcpts.VendorNotFoundException.class,
            CustomExcpts.InvalidPaymentMethodException.class,
            CustomExcpts.PaymentMethodNotFoundException.class,
            CustomExcpts.PaymentMethodNotActiveException.class,
            CustomExcpts.PaymentMethodNotSupportedException.class,
            CustomExcpts.MobileNetworkOperatorNotActiveException.class,
            CustomExcpts.PaymentGatewayException.class
    })
    public ResponseEntity<Object> handleOrderExceptions(Exception e) {
        log.warn("Global rate limit exception handler: {}", e.getMessage());
        return orderErrorHandler.handleError(e);
    }

    */
/**
     * Handles other payment-related exceptions
     *//*

    @ExceptionHandler({
            CustomExcpts.UnauthorizedException.class,
            CustomExcpts.OrderNotFoundException.class,
            CustomExcpts.PhoneNumberException.class,
            CustomExcpts.DatabaseOperationsException.class,
            CustomExcpts.TransactionNotFoundException.class,
            CustomExcpts.OperatorNotFoundException.class,
            CustomExcpts.VendorNotFoundException.class,
            CustomExcpts.InvalidPaymentMethodException.class,
            CustomExcpts.PaymentMethodNotFoundException.class,
            CustomExcpts.PaymentMethodNotActiveException.class,
            CustomExcpts.PaymentMethodNotSupportedException.class,
            CustomExcpts.MobileNetworkOperatorNotActiveException.class,
            CustomExcpts.PaymentGatewayException.class
    })
    public ResponseEntity<Object> handlePaymentExceptions(Exception e) {
        log.warn("Global payment exception handler: {}", e.getMessage());
        return paymentErrorHandler.handleError(e);
    }

    */
/**
     * Fallback handler for any other exceptions
     *//*

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception e) {
        log.error("Global generic exception handler: {}", e.getMessage(), e);
        return paymentErrorHandler.handleError(e);
    }
}
*/

package com.hybrid9.pg.Lipanasi.resources.excpts;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {

    private final PaymentErrorHandler paymentErrorHandler;
    private final OrderErrorHandler orderErrorHandler;

    /**
     * Handles rate limit exceeded exceptions globally
     * Routes to appropriate handler based on request context
     */
    @ExceptionHandler(CustomExcpts.RateLimitExceededException.class)
    public ResponseEntity<Object> handleRateLimitExceeded(
            CustomExcpts.RateLimitExceededException e,
            WebRequest request) {
        log.warn("Global rate limit handler - Rate limit exceeded: {}", e.getMessage());

        if (isOrderRelatedRequest(request)) {
            return orderErrorHandler.handleRateLimitError(e);
        } else {
            return paymentErrorHandler.handleRateLimitError(e);
        }
    }

   /* *//**
     * Handles order-specific exceptions
     *//*
    @ExceptionHandler({
            CustomExcpts.OrderNotFoundException.class
            // Add other order-specific exceptions here if needed
    })
    public ResponseEntity<Object> handleOrderExceptions(Exception e, WebRequest request) {
        log.warn("Global order exception handler: {}", e.getMessage());
        return orderErrorHandler.handleError(e);
    }

    *//**
     * Handles payment-specific exceptions
     *//*
    @ExceptionHandler({
            CustomExcpts.TransactionNotFoundException.class,
            CustomExcpts.PaymentGatewayException.class
    })
    public ResponseEntity<Object> handlePaymentExceptions(Exception e, WebRequest request) {
        log.warn("Global payment exception handler: {}", e.getMessage());
        return paymentErrorHandler.handleError(e);
    }*/

    /**
     * Handles common exceptions that can occur in both contexts
     * Routes based on request path or other context indicators
     */
    @ExceptionHandler({
            CustomExcpts.UnauthorizedException.class,
            CustomExcpts.OrderNotFoundException.class,
            CustomExcpts.PhoneNumberException.class,
            CustomExcpts.DatabaseOperationsException.class,
            CustomExcpts.OperatorNotFoundException.class,
            CustomExcpts.VendorNotFoundException.class,
            CustomExcpts.InvalidPaymentMethodException.class,
            CustomExcpts.TransactionNotFoundException.class,
            CustomExcpts.PaymentGatewayException.class,
            CustomExcpts.PaymentMethodNotFoundException.class,
            CustomExcpts.PaymentMethodNotActiveException.class,
            CustomExcpts.PaymentMethodNotSupportedException.class,
            CustomExcpts.PartnerValidationException.class,
            CustomExcpts.MobileNetworkOperatorNotActiveException.class,
    })
    public ResponseEntity<Object> handleCommonExceptions(Exception e, WebRequest request) {
        log.warn("Global common exception handler: {}", e.getMessage());

        if (isOrderRelatedRequest(request)) {
            return orderErrorHandler.handleError(e);
        } else {
            return paymentErrorHandler.handleError(e);
        }
    }

    /**
     * Fallback handler for any other exceptions
     * Routes based on request context
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception e, WebRequest request) {
        log.error("Global generic exception handler: {}", e.getMessage(), e);

        if (isOrderRelatedRequest(request)) {
            return orderErrorHandler.handleError(e);
        } else {
            return paymentErrorHandler.handleError(e);
        }
    }

    /**
     * Determines if the request is order-related based on URL path
     * You can customize this logic based on your API structure
     */
    private boolean isOrderRelatedRequest(WebRequest request) {
        String requestPath = request.getDescription(false);

        // Check if the request path contains order-related endpoints
        return requestPath.contains("/order") ||
                requestPath.contains("/orders") ||
                requestPath.contains("order-management") ||
                // Add more order-related path patterns as needed
                requestPath.contains("/api/v1/orders");
    }
}
