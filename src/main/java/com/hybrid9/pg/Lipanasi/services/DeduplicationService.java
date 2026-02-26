package com.hybrid9.pg.Lipanasi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.dto.PushUssdDto;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.repositories.pushussd.PushUssdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DeduplicationService {
    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);
    private static final String DEDUP_PREFIX = "dedup:tx:";

    private final Gson gson = new Gson();
    private final PushUssdRepository pushUssdRepository;

    @Autowired(required = false)
    @Qualifier("redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    public DeduplicationService(PushUssdRepository pushUssdRepository) {
        this.pushUssdRepository = pushUssdRepository;
    }

    /**
     * Check if a message has already been deposited and mark accordingly
     * Now with Redis-based fast path for recently processed transactions
     */
    @Transactional
    public String checkAndMarkDeposited(String pushUssdDtoStr) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        PushUssdDto message = mapper.readValue(pushUssdDtoStr, PushUssdDto.class);

        // Generate a deduplication key based on reference, msisdn, operator and amount
        String dedupKey = generateDedupKey(message);

        // Fast path - check Redis first if available
        if (redisTemplate != null) {
            Boolean keyExists = redisTemplate.hasKey(dedupKey);
            if (Boolean.TRUE.equals(keyExists)) {
                log.info("Fast path deduplication: Transaction already deposited for {}, from {}, with mobile number {}",
                        message.getReference(), message.getOperator(), message.getMsisdn());
                return null;  // Filtering out duplicate
            }
        }

        // Slow path - check database
        boolean alreadyDeposited = this.checkMessageHistory(
                message.getReference(),
                message.getMsisdn(),
                message.getOperator(),
                message.getAmount()
        );

        // If not duplicate and Redis is available, mark it in Redis for fast future lookups
        if (!alreadyDeposited && redisTemplate != null) {
            redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofHours(24));
        }

        if (alreadyDeposited) {
            log.info("Database deduplication: Transaction already deposited for {}, from {}, with mobile number {}",
                    message.getReference(), message.getOperator(), message.getMsisdn());
            return null;  // Filtering out duplicate
        }

        return mapper.writeValueAsString(message);
    }

    /**
     * Generate a consistent deduplication key based on transaction attributes
     */
    private String generateDedupKey(PushUssdDto message) {
        return DEDUP_PREFIX +
                message.getReference() + ":" +
                message.getMsisdn() + ":" +
                message.getOperator() + ":" +
                message.getAmount();
    }

    /**
     * Check transaction history in database and update status if needed
     */
    @Transactional
    private boolean checkMessageHistory(String reference, String msisdn, String operator, double amount) {
        AtomicBoolean alreadyDeposited = new AtomicBoolean(false);

        this.pushUssdRepository.findByReferenceAndMsisdnAndOperatorAndAmount(reference, msisdn, operator, amount)
                .ifPresent(pushUssd -> {
                    // Check if this transaction is already processed
                    if (pushUssd.getCollectionStatus() != CollectionStatus.COLLECTED &&
                            pushUssd.getCollectionStatus() != CollectionStatus.PROCESSING &&
                            pushUssd.getCollectionStatus() != CollectionStatus.NEW &&
                            pushUssd.getCollectionStatus() != CollectionStatus.FAILED) {

                        // Mark as duplicate and save
                        alreadyDeposited.set(true);
                        pushUssd.setCollectionStatus(CollectionStatus.DUPLICATE);
                        this.pushUssdRepository.save(pushUssd);
                        log.debug("Transaction marked as DUPLICATE: {}", reference);
                    }

                    /*else {
                        // Mark for deposit and save
                        alreadyDeposited.set(false);
                        pushUssd.setCollectionStatus(CollectionStatus.MARKED_FOR_DEPOSIT);
                        this.pushUssdRepository.save(pushUssd);
                        log.debug("Transaction marked as MARKED_FOR_DEPOSIT: {}", reference);
                    }*/
                });

        return alreadyDeposited.get();
    }
}