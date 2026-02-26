package com.hybrid9.pg.Lipanasi.enums;

import org.springframework.http.HttpStatus;

public enum ListOfErrorCode {

    // 0xx — Success Codes
    PAYMENT_SUCCESS("000", "Payment successful", HttpStatus.OK),
    PAYMENT_PENDING("001", "Payment pending", HttpStatus.OK),
    PAYMENT_INITIATED("002", "Payment initiated", HttpStatus.OK),

    // 3xx — Authentication & Authorization Errors
    UNAUTHORIZED("300", "Invalid session or authentication failed", HttpStatus.UNAUTHORIZED),
    INVALID_SESSION("301", "Session ID is invalid or expired", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED("302", "User session has expired", HttpStatus.UNAUTHORIZED),

    // 4xx — Bad Request & Validation Errors
    PARTNER_VALIDATION_FAILED("400", "Partner validation failed", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD("401", "The specified payment method is invalid", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_NOT_FOUND("402", "Payment method not available", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("403", "Invalid payment request parameters", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("404", "Required field is missing from the request", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT("405", "Payment amount is invalid or out of range", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER("406", "Phone number format is invalid", HttpStatus.BAD_REQUEST),
    INVALID_VENDOR_INFO("407", "Vendor information is invalid", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("408", "Request validation failed", HttpStatus.BAD_REQUEST),
    CURRENCY_NOT_SUPPORTED("409", "Currency is not supported", HttpStatus.BAD_REQUEST),
    COUNTRY_NOT_SUPPORTED("410", "Country is not supported for this payment method", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_NOT_ACTIVE("411", "Payment method is not active", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_NOT_SUPPORTED("412", "Payment method is not supported", HttpStatus.BAD_REQUEST),

    // 5xx — Payment Processing Errors
    PAYMENT_PROCESSING_ERROR("500", "Payment processing failed", HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_FAILED("501", "Payment failed", HttpStatus.UNPROCESSABLE_ENTITY),
    INSUFFICIENT_FUNDS("502", "Insufficient funds in the account", HttpStatus.UNPROCESSABLE_ENTITY),
    TRANSACTION_DECLINED("503", "Transaction was declined by the payment provider", HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_TIMEOUT("504", "Payment request timed out", HttpStatus.REQUEST_TIMEOUT),
    DUPLICATE_TRANSACTION("505", "Duplicate transaction detected", HttpStatus.CONFLICT),
    TRANSACTION_STATE_ERROR("506", "Invalid transaction state", HttpStatus.CONFLICT),

    // 6xx — Not Found Errors
    ORDER_NOT_FOUND("600", "The specified order could not be found", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND("601", "Transaction not found", HttpStatus.NOT_FOUND),
    OPERATOR_NOT_FOUND("602", "Mobile network operator not supported", HttpStatus.NOT_FOUND),
    VENDOR_NOT_FOUND("603", "Payment vendor not found", HttpStatus.NOT_FOUND),

    // 7xx — Network/Operator Specific Errors
    OPERATOR_UNAVAILABLE("700", "Mobile network operator is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    NETWORK_ERROR("701", "Network communication error with payment provider", HttpStatus.BAD_GATEWAY),
    CONFIGURATION_ERROR("702", "Payment configuration error", HttpStatus.INTERNAL_SERVER_ERROR),
    MOBILE_NETWORK_OPERATOR_NOT_ACTIVE("703", "Mobile network operator is not active", HttpStatus.SERVICE_UNAVAILABLE),

    // 8xx — System/Internal Errors
    INTERNAL_SERVER_ERROR("800", "An unexpected error occurred while processing the payment", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("801", "Payment service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_ERROR("802", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // 9xx — Rate Limiting & Unknown Errors
    RATE_LIMIT_EXCEEDED("900", "Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    UNKNOWN_ERROR("999", "Unknown error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ListOfErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getStatusCode() {
        return httpStatus.value();
    }

    // Helper method to get error code from exception type
    public static ListOfErrorCode fromException(Throwable exception) {
        String exceptionName = exception.getClass().getSimpleName();

        return switch (exceptionName) {
            case "UnauthorizedException" -> UNAUTHORIZED;
            case "RateLimitExceededException" -> RATE_LIMIT_EXCEEDED;
            case "OrderNotFoundException" -> ORDER_NOT_FOUND;
            case "OperatorNotFoundException" -> OPERATOR_NOT_FOUND;
            case "VendorNotFoundException" -> VENDOR_NOT_FOUND;
            case "InvalidPaymentMethodException" -> INVALID_PAYMENT_METHOD;
            case "PaymentMethodNotFoundException" -> PAYMENT_METHOD_NOT_FOUND;
            default -> INTERNAL_SERVER_ERROR;
        };
    }

    // Helper method to create error response
    public PaymentErrorResponse toErrorResponse() {
        return new PaymentErrorResponse(this.code, this.message, this.httpStatus.value());
    }

    // Inner class for structured error response
    public static class PaymentErrorResponse {
        private final String errorCode;
        private final String errorMessage;
        private final int statusCode;

        public PaymentErrorResponse(String errorCode, String errorMessage, int statusCode) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.statusCode = statusCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
