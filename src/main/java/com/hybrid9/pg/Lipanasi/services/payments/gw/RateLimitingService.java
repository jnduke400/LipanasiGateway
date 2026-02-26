package com.hybrid9.pg.Lipanasi.services.payments.gw;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RateLimitingService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> rateLimitScript;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    public RateLimitingService(RedisTemplate<String, Object> redisTemplate,
                               StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;

        // Load Redis Lua script for rate limiting
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/rate-limiter.lua")));
        script.setResultType(Long.class);
        this.rateLimitScript = script;
    }

    /**
     * Fixed window rate limiting
     */
    public boolean checkFixedWindowRateLimit(String key, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + "fixed:" + key + ":" + windowSeconds;

        Long currentCount = stringRedisTemplate.opsForValue().increment(redisKey);

        // Set expiry if this is a new key
        if (currentCount != null && currentCount == 1) {
            stringRedisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }

        return currentCount != null && currentCount <= maxRequests;
    }

    /**
     * Sliding window rate limiting
     */
    public boolean checkSlidingWindowRateLimit(String key, int maxRequests, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + "sliding:" + key;
        long currentTime = System.currentTimeMillis();

        // Execute as Redis transaction for atomicity
        return redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                // Add the current timestamp
                operations.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);

                // Remove old entries outside the window
                operations.opsForZSet().removeRangeByScore(redisKey, 0, currentTime - (windowSeconds * 1000));

                // Get the count of requests in the current window
                operations.opsForZSet().size(redisKey);

                // Set expiry to clean up eventually
                operations.expire(redisKey, windowSeconds * 2, TimeUnit.SECONDS);

                // Execute transaction and get results
                List<Object> results = operations.exec();

                if (results != null && results.size() >= 3) {
                    Long count = (Long) results.get(2);
                    return count <= maxRequests;
                }

                return false;
            }
        });
    }

    /**
     * Token bucket rate limiting using Lua script
     */
    public boolean checkTokenBucketRateLimit(String key, int capacity, int refillRate, int windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + "token:" + key;
        long currentTimeMillis = System.currentTimeMillis();

        List<String> keys = Collections.singletonList(redisKey);
        Long remainingTokens = stringRedisTemplate.execute(
                rateLimitScript,
                keys,
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(currentTimeMillis),
                String.valueOf(windowSeconds * 1000)
        );

        boolean allowed = remainingTokens != null && remainingTokens > 0;
        if (!allowed) {
            log.debug("Rate limit exceeded for key: {}", key);
        }

        return allowed;
    }

    /**
     * IP-based rate limiting
     */
    public boolean checkIpRateLimit(String ipAddress, int maxRequests, int windowSeconds) {
        return checkSlidingWindowRateLimit("ip:" + ipAddress, maxRequests, windowSeconds);
    }

    /**
     * User-based rate limiting
     */
    public boolean checkUserRateLimit(String userId, int maxRequests, int windowSeconds) {
        return checkSlidingWindowRateLimit("user:" + userId, maxRequests, windowSeconds);
    }

    /**
     * API endpoint rate limiting
     */
    public boolean checkApiRateLimit(String apiEndpoint, String clientId, int maxRequests, int windowSeconds) {
        return checkSlidingWindowRateLimit("api:" + apiEndpoint + ":" + clientId, maxRequests, windowSeconds);
    }

    /**
     * Payment transaction rate limiting
     */
    public boolean checkPaymentRateLimit(String merchantId, int maxRequests, int windowSeconds) {
        return checkSlidingWindowRateLimit("payment:" + merchantId, maxRequests, windowSeconds);
    }
}
