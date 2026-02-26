package com.hybrid9.pg.Lipanasi.services;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing idempotency of transaction processing
 * Uses Redis to track recently processed transactions
 */
@Service
public class IdempotencyBankService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyBankService.class);
    private static final String PROCESSING_PREFIX = "processing:bank:";
    private static final String COMPLETED_PREFIX = "completed:bank:";
    private static final String DEDUP_PREFIX = "dedup:bank:tx:";

    @Autowired(required = false)
    @Qualifier("redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Filter out records that have been processed recently
     *
     * @param records       List of CardPayment records to filter
     * @param windowSeconds Time window in seconds to consider records as recently processed
     * @return Filtered list of records that haven't been processed recently
     */
    public List<CardPayment> filterProcessedRecords(List<CardPayment> records, int windowSeconds) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // If Redis is not available, return all records
        if (redisTemplate == null) {
            log.warn("Redis not available for idempotency checks, processing all records");
            return records;
        }

        return records.stream()
                .filter(record -> {
                    // Check if the record is already marked as processing or completed
                    String processingKey = PROCESSING_PREFIX + record.getId();
                    String completedKey = COMPLETED_PREFIX + record.getId();

                    // Also check the composite business key for deduplication
                    String dedupKey = generateDedupKey(record);

                    Boolean isProcessing = redisTemplate.hasKey(processingKey);
                    Boolean isCompleted = redisTemplate.hasKey(completedKey);
                    Boolean isDuplicate = redisTemplate.hasKey(dedupKey);

                    boolean shouldProcess = !(Boolean.TRUE.equals(isProcessing) ||
                            Boolean.TRUE.equals(isCompleted) ||
                            Boolean.TRUE.equals(isDuplicate));

                    if (!shouldProcess) {
                        log.debug("Filtering out record ID: {}, Reference: {} - Already being processed or completed",
                                record.getId(), record.getPaymentReference());
                    }

                    return shouldProcess;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generate a deduplication key based on business attributes
     */
    private String generateDedupKey(CardPayment record) {
        return DEDUP_PREFIX +
                record.getPaymentReference() + ":" +
                record.getTransactionId() + ":" +
                record.getBankId() + ":" +
                record.getAmount();
    }

    /**
     * Mark records as currently being processed
     *
     * @param records List of CardPayment records to mark as processing
     */
    public void markRecordsAsProcessing(List<CardPayment> records, int processingTimeoutMinutes) {
        if (records == null || records.isEmpty() || redisTemplate == null) {
            return;
        }

        records.forEach(record -> {
            // Mark by ID
            String key = PROCESSING_PREFIX + record.getId();
            redisTemplate.opsForValue().set(key, record.getPaymentReference(), Duration.ofMinutes(processingTimeoutMinutes));

            // Also mark by business key if status is appropriate
            if (record.getCollectionStatus() == CollectionStatus.PROCESSING ||
                    record.getCollectionStatus() == CollectionStatus.MARKED_FOR_DEPOSIT) {
                String dedupKey = generateDedupKey(record);
                redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofHours(1));
            }

            log.debug("Marked record as processing: ID {}, Reference {}", record.getId(), record.getPaymentReference());
        });
    }

    /**
     * Mark a record as completed (no longer processing)
     *
     * @param record CardPayment record to mark as completed
     */
    public void markRecordAsCompleted(CardPayment record, int dedupExpiryHours) {
        if (record == null || redisTemplate == null) {
            return;
        }

        // Remove processing flag
        String processingKey = PROCESSING_PREFIX + record.getId();
        redisTemplate.delete(processingKey);

        // Add completed flag with expiration time
        String completedKey = COMPLETED_PREFIX + record.getId();
        redisTemplate.opsForValue().set(completedKey, record.getPaymentReference(), Duration.ofHours(dedupExpiryHours));

        // Also mark by business key
        String dedupKey = generateDedupKey(record);
        redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofHours(24));

        log.debug("Marked record as completed: ID {}, Reference {}", record.getId(), record.getPaymentReference());
    }

    /**
     * Check if a record has already been processed
     *
     * @param record CardPayment record to check
     * @return true if the record has been processed, false otherwise
     */
    public boolean isRecordProcessed(CardPayment record) {
        if (record == null || redisTemplate == null) {
            return false;
        }

        String processingKey = PROCESSING_PREFIX + record.getId();
        String completedKey = COMPLETED_PREFIX + record.getId();
        String dedupKey = generateDedupKey(record);

        Boolean isProcessing = redisTemplate.hasKey(processingKey);
        Boolean isCompleted = redisTemplate.hasKey(completedKey);
        Boolean isDuplicate = redisTemplate.hasKey(dedupKey);

        return Boolean.TRUE.equals(isProcessing) ||
                Boolean.TRUE.equals(isCompleted) ||
                Boolean.TRUE.equals(isDuplicate);
    }

    /**
     * Check if a record has already been processed by ID
     *
     * @param recordId Record ID to check
     * @return true if the record has been processed, false otherwise
     */
    public boolean isRecordProcessed(String recordId) {
        if (recordId == null || redisTemplate == null) {
            return false;
        }

        String processingKey = PROCESSING_PREFIX + recordId;
        String completedKey = COMPLETED_PREFIX + recordId;

        Boolean isProcessing = redisTemplate.hasKey(processingKey);
        Boolean isCompleted = redisTemplate.hasKey(completedKey);

        return Boolean.TRUE.equals(isProcessing) || Boolean.TRUE.equals(isCompleted);
    }

    public void markRecordAsProcessing(CardPayment record, int processingTimeoutMinutes) {
        if (record == null || record.getId() == null || redisTemplate == null) {
            return;
        }

        // Mark by ID
        String key = PROCESSING_PREFIX + record.getId();
        redisTemplate.opsForValue().set(key, record.getPaymentReference(), Duration.ofMinutes(processingTimeoutMinutes));

        // Also mark by business key if status is appropriate
        if (record.getCollectionStatus() == CollectionStatus.PROCESSING ||
                record.getCollectionStatus() == CollectionStatus.MARKED_FOR_DEPOSIT) {
            String dedupKey = generateDedupKey(record);
            redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofHours(1));
        }

        log.debug("Marked record as processing: ID {}, Reference {}", record.getId(), record.getPaymentReference());

    }
}
