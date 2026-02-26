package com.hybrid9.pg.Lipanasi.route.processor.circuitbreaker;


import com.hybrid9.pg.Lipanasi.component.ServiceNameComponent;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HalopesaCircuitBreakerProcessor implements Processor {

    private final CircuitBreaker circuitBreaker;
    private final CashInLogService cashInLogService;
    private final DepositService depositService;
    private final ServiceNameComponent serviceName;

    public HalopesaCircuitBreakerProcessor(@Qualifier("halopesaCircuitBreaker") CircuitBreaker circuitBreaker,
                                        CashInLogService cashInLogService,
                                        DepositService depositService,
                                        ServiceNameComponent serviceName) {
        this.circuitBreaker = circuitBreaker;
        this.cashInLogService = cashInLogService;
        this.depositService = depositService;
        this.serviceName = serviceName;
    }

    /**
     * This method processes the exchange and checks the state of the circuit breaker.
     * If the circuit breaker is open, it updates the cash in log and deposit status
     * and throws an exception to stop further processing.
     *
     * @param exchange The Camel exchange object
     * @throws Exception if the circuit breaker is open
     */

    @Override
    public void process(Exchange exchange) throws Exception {
        CircuitBreaker.State state = circuitBreaker.getState();

        if (state == CircuitBreaker.State.OPEN) {
            // Circuit is open - fail fast
            String cashInLogId = exchange.getProperty("cashInLogId", String.class);
            String transactionId = exchange.getProperty("transactionId", String.class);

            // Update the cash in log
            if (cashInLogId != null) {
                CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                if (cashInLog != null) {
                    cashInLog.setStatus(RequestStatus.SERVICE_UNAVAILABLE);
                    cashInLog.setErrorMessage(getServiceName(serviceName) + " service is currently unavailable (Circuit Open)");
                    cashInLogService.recordLog(cashInLog);
                }
            }

            // Update the deposit
            if (transactionId != null) {
                depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                    deposit.setRequestStatus(RequestStatus.SERVICE_UNAVAILABLE);
                    deposit.setErrorMessage(getServiceName(serviceName) + " service is currently unavailable. Your transaction will be retried later.");
                    depositService.recordDeposit(deposit);
                });
            }

            // Stop further processing by throwing exception
            throw new CircuitBreakerOpenException(getServiceName(serviceName) + " service circuit breaker is OPEN");
        }

        // Otherwise, let the request proceed
        exchange.setProperty("circuitBreakerState", state.toString());
    }

    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    private String getServiceName(ServiceNameComponent serviceName) {
        return switch (serviceName.getServiceName()) {
            case MIXX_PAYMENT -> "Mixx Payment";
            case MPESA_PAYMENT -> "Mpesa Payment";
            case AIRTEL_PAYMENT -> "Airtel Payment";
            case HALOTEL_PAYMENT -> "Halotel Payment";
            case AM_TQS_PAYMENT -> "AM TQS Payment";
            default -> null;
        };
    }
}
