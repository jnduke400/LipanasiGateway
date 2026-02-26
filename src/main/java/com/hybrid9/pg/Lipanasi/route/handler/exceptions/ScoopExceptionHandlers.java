package com.hybrid9.pg.Lipanasi.route.handler.exceptions;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
@Component
public class ScoopExceptionHandlers {
    // Helper method to determine if an exception is retryable
    /*public boolean isRetryableException(Exception exception) {
        if (exception instanceof HttpClientErrorException clientError) {
            // Don't retry client errors (4xx) - these are usually permanent
            return switch (clientError.getStatusCode().value()) {
                case 400, 401, 403, 404, 409, 422 -> false; // Bad Request, Unauthorized, Forbidden, Not Found, Conflict, Unprocessable Entity
                case 429 -> true; // Too Many Requests - can retry with backoff
                default -> false; // Other 4xx errors are generally not retryable
            };
        }

        if (exception instanceof HttpServerErrorException serverError) {
            // Some server errors are retryable, others are not
            return switch (serverError.getStatusCode().value()) {
                case 502, 503, 504 -> true; // Bad Gateway, Service Unavailable, Gateway Timeout
                case 500 -> true; // Internal Server Error - might be temporary
                default -> false;
            };
        }

        // Network-related exceptions are retryable
        return exception instanceof ConnectException
                || exception instanceof SocketTimeoutException;
    }*/

    public boolean isRetryableException(Exception exception) {
        Integer statusCode = null;

        // Extract status code from HTTP exceptions
        if (exception instanceof HttpClientErrorException clientError) {
            statusCode = clientError.getStatusCode().value();
        }
        else if (exception instanceof HttpServerErrorException serverError) {
            statusCode = serverError.getStatusCode().value();
        }
        else if (exception instanceof HttpOperationFailedException httpFailed) {
            statusCode = httpFailed.getStatusCode();
        }

        // Evaluate retryability based on status code
        if (statusCode != null) {
            return switch (statusCode) {
                // Client errors - generally not retryable
                case 400, 401, 403, 404, 409, 422 -> false;

                // Rate limiting - retryable with backoff
                case 429 -> true;

                // Server errors - retryable as they might be temporary
                case 500, 502, 503, 504 -> true;

                // All other HTTP errors (including other 4xx) - not retryable
                default -> statusCode >= 500; // Retry any other 5xx codes
            };
        }

        // Network-related exceptions are retryable
        return exception instanceof ConnectException
                || exception instanceof SocketTimeoutException;
    }

    // Helper method to determine if an exception should trigger circuit breaker
    public boolean shouldTriggerCircuitBreaker(Exception exception) {
        Integer statusCode = null;

        // Extract status code from HTTP exceptions
        if (exception instanceof HttpClientErrorException clientError) {
            statusCode = clientError.getStatusCode().value();
        }
        else if (exception instanceof HttpServerErrorException serverError) {
            statusCode = serverError.getStatusCode().value();
        }
        else if (exception instanceof HttpOperationFailedException httpFailed) {
            statusCode = httpFailed.getStatusCode();
        }

        // Evaluate if status code should trigger circuit breaker
        if (statusCode != null) {
            return switch (statusCode) {
                // Rate limiting might indicate service issues
                case 429 -> true;

                // Server errors indicate service availability issues
                case 500, 502, 503, 504 -> true;

                // Other client errors are not service availability issues
                default -> statusCode >= 500; // Any other 5xx codes should trigger circuit breaker
            };
        }

        // Network-related exceptions should trigger circuit breaker
        return exception instanceof ConnectException
                || exception instanceof SocketTimeoutException;
    }

    // Helper method to classify exceptions
    public RequestStatus classifyException(Exception exception) {
        if (exception instanceof ConnectException) {
            return RequestStatus.NETWORK_CONNECTION_ISSUE;
        } else if (exception instanceof SocketTimeoutException) {
            return RequestStatus.TIMEOUT;
        } else if (exception instanceof HttpServerErrorException.ServiceUnavailable) {
            return RequestStatus.SERVICE_UNAVAILABLE;
        } else if (exception instanceof HttpClientErrorException clientError) {
            return switch (clientError.getStatusCode().value()) {
                case 401, 403 -> RequestStatus.INVALID_CREDENTIALS;
                case 400, 422 -> RequestStatus.INVALID_REQUEST;
                case 409 -> RequestStatus.BUSINESS_RULE_VIOLATION;
                case 429 -> RequestStatus.SERVICE_UNAVAILABLE; // Rate limited
                default -> RequestStatus.FAILED;
            };
        }else if (exception instanceof HttpOperationFailedException httpFailed) {
            // Handle Camel's HttpOperationFailedException
            return classifyHttpResponseCode(httpFailed.getStatusCode());
        }
        return RequestStatus.FAILED;
    }

    // Helper method to classify HTTP response codes
    public RequestStatus classifyHttpResponseCode(Integer responseCode) {
        if (responseCode == null) {
            return RequestStatus.NETWORK_CONNECTION_ISSUE;
        }

        return switch (responseCode) {
            case 400, 422 -> RequestStatus.INVALID_REQUEST;
            case 401, 403 -> RequestStatus.INVALID_CREDENTIALS;
            case 404 -> RequestStatus.RESOURCE_NOT_FOUND;
            case 409 -> RequestStatus.BUSINESS_RULE_VIOLATION;
            case 429 -> RequestStatus.SERVICE_UNAVAILABLE;
            case 500, 502, 503, 504 -> RequestStatus.SERVICE_UNAVAILABLE;
            default -> RequestStatus.FAILED;
        };
    }
}
