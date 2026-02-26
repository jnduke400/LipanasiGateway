package com.hybrid9.pg.Lipanasi.resources.excpts;

import org.springframework.stereotype.Component;

import java.io.Serial;

@Component
public class CustomExcpts {

    public static class PhoneNumberException extends RuntimeException {
        public PhoneNumberException(String message) {
            super(message);
        }
    }

    public static class DatabaseOperationsException extends RuntimeException {
        public DatabaseOperationsException(String message) {
            super(message);
        }
    }

    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }
    }

    public static class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    /*public static class RateLimitExceededException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        public RateLimitExceededException(String message) {
            super(message);
        }
    }*/

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String s) {
            super(s);
        }
    }

    public static class OperatorNotFoundException extends RuntimeException {
        public OperatorNotFoundException(String s) {
            super(s);
        }
    }

    public static class VendorNotFoundException extends RuntimeException {
        public VendorNotFoundException(String s) {
            super(s);
        }
    }

    public static class InvalidPaymentMethodException extends RuntimeException {
        public InvalidPaymentMethodException(String s) {
            super(s);
        }
    }

    public static class PaymentMethodNotFoundException extends RuntimeException {
        public PaymentMethodNotFoundException(String s) {
            super(s);
        }
    }

    public static class PaymentMethodNotActiveException extends RuntimeException {
        public PaymentMethodNotActiveException(String message) {
            super(message);
        }
    }

    public static class PaymentMethodNotSupportedException extends RuntimeException {
        public PaymentMethodNotSupportedException(String message) {
            super(message);
        }
    }

    public static class MobileNetworkOperatorNotActiveException extends RuntimeException {
        public MobileNetworkOperatorNotActiveException(String message) {
            super(message);
        }

    }


    public static class RateLimitExceededException extends RuntimeException {
        private final int maxRequests;
        private final int timeWindowSeconds;
        private final String rateLimitType;

        public RateLimitExceededException(String message) {
            super(message);
            this.maxRequests = 0;
            this.timeWindowSeconds = 0;
            this.rateLimitType = "UNKNOWN";
        }

        public RateLimitExceededException(String message, int maxRequests, int timeWindowSeconds, String rateLimitType) {
            super(message);
            this.maxRequests = maxRequests;
            this.timeWindowSeconds = timeWindowSeconds;
            this.rateLimitType = rateLimitType;
        }

        public RateLimitExceededException(String message, Throwable cause) {
            super(message, cause);
            this.maxRequests = 0;
            this.timeWindowSeconds = 0;
            this.rateLimitType = "UNKNOWN";
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public int getTimeWindowSeconds() {
            return timeWindowSeconds;
        }

        public String getRateLimitType() {
            return rateLimitType;
        }
    }

    public static class PartnerValidationException extends RuntimeException {
        public PartnerValidationException(String message) {
            super(message);
        }
    }
}
