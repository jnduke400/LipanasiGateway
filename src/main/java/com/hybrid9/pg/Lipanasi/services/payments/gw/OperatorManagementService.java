package com.hybrid9.pg.Lipanasi.services.payments.gw;

import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OperatorManagementService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String OPERATOR_PREFIX = "operator:";
    private static final String OPERATOR_MAPPING_PREFIX = "operator-mapping:";
    private static final int DEFAULT_OPERATOR_EXPIRY = 30; // minutes

    public OperatorManagementService(RedisTemplate<String, Object> redisTemplate,
                                     StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Save operator details in Redis.
     *
     * @param OperatorMapping the operator
     */

    public String createOperator(OperatorMapping operator) {
        String key = OPERATOR_PREFIX + operator.getOperatorPrefix();

        // Set creation time if not already set
        if (operator.getCreatedAt() == null) {
            operator.setCreatedAt(LocalDateTime.now());
        }
        operator.setLastAccessedAt(LocalDateTime.now());

        // Store the operator details in Redis
        redisTemplate.opsForValue().set(key, operator);
        stringRedisTemplate.expire(key, DEFAULT_OPERATOR_EXPIRY, TimeUnit.MINUTES);

        // Create index by operator ID for quick access
        if (operator.getOperatorId() != null) {
            String operatorId = operator.getOperatorId();
            stringRedisTemplate.opsForSet().add(OPERATOR_MAPPING_PREFIX + operatorId, operator.getOperatorPrefix());
            stringRedisTemplate.expire(OPERATOR_MAPPING_PREFIX + operatorId, DEFAULT_OPERATOR_EXPIRY, TimeUnit.MINUTES);
        }
        log.info("Created new operator: {} for prefix: {}", operator.getOperatorName(), operator.getOperatorPrefix());
        return operator.getOperatorPrefix();
    }

    public Optional<OperatorMapping> getOperator(String operatorPrefix) {
        String key = OPERATOR_PREFIX + operatorPrefix;
        OperatorMapping operator = (OperatorMapping) redisTemplate.opsForValue().get(key);
        if (operator != null) {
            // Update last accessed time
            operator.setLastAccessedAt(LocalDateTime.now());

            redisTemplate.opsForValue().set(key, operator);
            stringRedisTemplate.expire(key, DEFAULT_OPERATOR_EXPIRY, TimeUnit.MINUTES);

            log.info("Retrieved operator: {} for Prefix: {}", operator.getOperatorName(), operatorPrefix);
            return Optional.of(operator);
        } else {
            log.warn("Operator not found for Prefix:- {}", operatorPrefix);
            return Optional.empty();
        }
        //return Optional.empty();
    }

    /**
     * Update an existing operator
     */
    public void updateOperator(String operatorId, OperatorMapping operator) {
        String key = OPERATOR_PREFIX + operatorId;
        operator.updateLastAccessed();
        redisTemplate.opsForValue().set(key, operator);
        redisTemplate.expire(key, DEFAULT_OPERATOR_EXPIRY, TimeUnit.MINUTES);
    }
}
