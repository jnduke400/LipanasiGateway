package com.hybrid9.pg.Lipanasi.rest.healthcheck;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

//@RestController
//@RequestMapping("/api/v1/healthcheck/")
@Slf4j
public class HealthcheckController {
    private final CircuitBreaker airtelCircuitBreaker;
    private final CircuitBreaker mpesaCircuitBreaker;
    private final CircuitBreaker mixxCircuitBreaker;
    private final CircuitBreaker halopesaCircuitBreaker;
    // Add other circuit breakers you want to monitor

    public HealthcheckController(
            @Qualifier("airtelCircuitBreaker") CircuitBreaker airtelCircuitBreaker,
            @Qualifier("mpesaCircuitBreaker") CircuitBreaker mpesaCircuitBreaker,
            @Qualifier("mixxCircuitBreaker") CircuitBreaker mixxCircuitBreaker,
            @Qualifier("halopesaCircuitBreaker") CircuitBreaker halopesaCircuitBreaker
            // Add other circuit breakers as parameters
    ) {
        this.airtelCircuitBreaker = airtelCircuitBreaker;
        this.mpesaCircuitBreaker = mpesaCircuitBreaker;
        this.mixxCircuitBreaker = mixxCircuitBreaker;
        this.halopesaCircuitBreaker = halopesaCircuitBreaker;
        // Initialize other circuit breakers
    }

    @GetMapping("/circuit-breaker")
    public Map<String, Map<String, Object>> getAllCircuitBreakersHealth() {
        Map<String, Map<String, Object>> allHealthInfo = new LinkedHashMap<>();

        allHealthInfo.put("airtel", getCircuitBreakerHealth(airtelCircuitBreaker));
        allHealthInfo.put("mpesa", getCircuitBreakerHealth(mpesaCircuitBreaker));
        allHealthInfo.put("mixx", getCircuitBreakerHealth(mixxCircuitBreaker));
        allHealthInfo.put("halopesa", getCircuitBreakerHealth(halopesaCircuitBreaker));
        // Add other circuit breakers

        return allHealthInfo;
    }

    private Map<String, Object> getCircuitBreakerHealth(CircuitBreaker circuitBreaker) {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", circuitBreaker.getState().name());
        healthInfo.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        healthInfo.put("bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        healthInfo.put("failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        healthInfo.put("notPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
        return healthInfo;
    }
}
