package com.hybrid9.pg.Lipanasi.services;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerService.class);

    private final CircuitBreakerRegistry registry;
    private final Map<String, CircuitBreaker.State> lastKnownStates = new ConcurrentHashMap<>();

    public CircuitBreakerService(CircuitBreakerRegistry registry) {
        this.registry = registry;

        // Initialize state tracking and register event listeners
        registry.getAllCircuitBreakers().forEach(cb -> {
            String name = cb.getName();
            lastKnownStates.put(name, cb.getState());

            // Add state transition listeners for logging
            cb.getEventPublisher().onStateTransition(event -> {
                StateTransition transition = event.getStateTransition();
                log.warn("Circuit breaker '{}' state changed: {} -> {}",
                        name, transition.getFromState(), transition.getToState());
                lastKnownStates.put(name, transition.getToState());
            });

            // Add success/failure event listeners for detailed monitoring
            cb.getEventPublisher().onSuccess(event ->
                    log.debug("Circuit breaker '{}' recorded successful call: {}ms",
                            name, event.getElapsedDuration().toMillis()));

            cb.getEventPublisher().onError(event ->
                    log.error("Circuit breaker '{}' recorded failed call: {}ms, error: {}",
                            name, event.getElapsedDuration().toMillis(),
                            event.getThrowable().getMessage()));
        });
    }

    public CircuitBreaker.State getCircuitState(String name) {
        CircuitBreaker breaker = registry.circuitBreaker(name);
        return breaker.getState();
    }

    public void recordSuccess(String name) {
        CircuitBreaker breaker = registry.circuitBreaker(name);
        breaker.onSuccess(0, TimeUnit.SECONDS);
        log.debug("Success recorded for circuit '{}'. State: {}, Failure Rate: {}%",
                name, breaker.getState(), breaker.getMetrics().getFailureRate());
    }

    public void recordFailure(String name, Throwable throwable) {
        CircuitBreaker breaker = registry.circuitBreaker(name);
        breaker.onError(0,TimeUnit.SECONDS, throwable);
        log.warn("Failure recorded for circuit '{}'. Error: {}. State: {}, Failure Rate: {}%",
                name, throwable.getMessage(), breaker.getState(),
                breaker.getMetrics().getFailureRate());
    }

    // Health monitoring task
    @Scheduled(fixedRate = 60000) // Every minute
    public void reportCircuitStates() {
        registry.getAllCircuitBreakers().forEach(cb -> {
            String name = cb.getName();
            CircuitBreaker.State state = cb.getState();
            CircuitBreaker.Metrics metrics = cb.getMetrics();

            log.info("Circuit '{}' state: {}, failure rate: {}%, slow call rate: {}%, " +
                            "successful: {}, failed: {}, slow: {}",
                    name, state, metrics.getFailureRate(), metrics.getSlowCallRate(),
                    metrics.getNumberOfSuccessfulCalls(), metrics.getNumberOfFailedCalls(),
                    metrics.getNumberOfSlowCalls());
        });
    }
}
