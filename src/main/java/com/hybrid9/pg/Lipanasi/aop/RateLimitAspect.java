package com.hybrid9.pg.Lipanasi.aop;

import com.hybrid9.pg.Lipanasi.annotationx.RateLimit;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.services.payments.gw.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    private final RateLimitingService rateLimitingService;
    private final HttpServletRequest request;

    public RateLimitAspect(RateLimitingService rateLimitingService,
                           HttpServletRequest request) {
        this.rateLimitingService = rateLimitingService;
        this.request = request;
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String clientId = extractClientId();
        String apiPath = request.getRequestURI();

        boolean allowed = switch (rateLimit.type()) {
            case IP -> {
                String ipAddress = getClientIp();
                yield rateLimitingService.checkIpRateLimit(
                        ipAddress, rateLimit.maxRequests(), rateLimit.timeWindow());
            }
            case USER -> {
                String userId = getUserId();
                yield rateLimitingService.checkUserRateLimit(
                        userId, rateLimit.maxRequests(), rateLimit.timeWindow());
            }
            default -> rateLimitingService.checkApiRateLimit(
                    apiPath, clientId, rateLimit.maxRequests(), rateLimit.timeWindow());
        };

        if (!allowed) {
            log.warn("Rate limit exceeded. Type: {}, Client: {}, API: {}",
                    rateLimit.type(), clientId, apiPath);
            throw new CustomExcpts.RateLimitExceededException("Rate limit exceeded");
        }

        return joinPoint.proceed();
    }

    private String extractClientId() {
        // Extract from request
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return apiKey;
        }

        // Fall back to session
        HttpSession session = request.getSession(false);
        if (session != null) {
            return session.getId();
        }

        return getClientIp();
    }

    private String getUserId() {
        // Get from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) auth.getPrincipal()).getUsername();
        }

        return "anonymous";
    }

    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
