package com.hybrid9.pg.Lipanasi.configs;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(70) // More tolerant threshold (from current 50)
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Longer cooldown (from 30s)
                .permittedNumberOfCallsInHalfOpenState(10) // More test calls (from 5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED) // Better for bursty traffic
                .slidingWindowSize(30) // 30 seconds window (instead of call count)
                .minimumNumberOfCalls(20) // Need more calls to calculate rate (from 5)
                .slowCallRateThreshold(30) // Consider slow calls as potential failures
                .slowCallDurationThreshold(Duration.ofMillis(3000)) // Calls >3s are slow
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean(name = "gepgCircuitBreaker")
    public CircuitBreaker gepgCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("gepgPaymentService");
    }

    @Bean(name = "bankCircuitBreaker")
    public CircuitBreaker bankCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("bankPaymentService");
    }

    @Bean(name = "vendorCircuitBreaker")
    public CircuitBreaker vendorCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("vendorPaymentService");
    }

    @Bean(name = "traCircuitBreaker")
    public CircuitBreaker traCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("traPaymentService");
    }

    @Bean(name = "amTqsCircuitBreaker")
    public CircuitBreaker amTqsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("amTqsPaymentService");
    }

    @Bean(name = "mixxTqsCircuitBreaker")
    public CircuitBreaker mixxTqsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("mixxTqsPaymentService");
    }

    @Bean(name = "mpesaTqsCircuitBreaker")
    public CircuitBreaker mpesaTqsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("mpesaTqsPaymentService");
    }

    @Bean(name = "halopesaTqsCircuitBreaker")
    public CircuitBreaker halopesaTqsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("halopesaTqsPaymentService");
    }

    @Bean(name = "partnerDataCircuitBreaker")
    public CircuitBreaker partnerDataCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("partnerDataQuerying");
    }

    @Bean(name = "mixxCircuitBreaker")
    public CircuitBreaker mixxCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("mixxPaymentService");
    }

    @Bean(name = "airtelCircuitBreaker")
    public CircuitBreaker airtelCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("airtelPaymentService");
    }

    @Bean(name = "mpesaCircuitBreaker")
    public CircuitBreaker mpesaCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("mpesaPaymentService");
    }

    @Bean(name = "halopesaCircuitBreaker")
    public CircuitBreaker halopesaCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("halopesaPaymentService");
    }
}