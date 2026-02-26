package com.hybrid9.pg.Lipanasi.utilities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.security.SecureRandom;

/**
 * Professional order number generator that creates unique, traceable order numbers
 * with various format options for different business needs.
 *
 * Features:
 * - Date/time inclusion for chronological ordering
 * - Instance ID support for multi-instance deployments
 * - Sequence numbers for uniqueness
 * - Checksum validation for integrity
 * - Multiple format options (full, simple, compact)
 * - Format validation utilities
 */

@Slf4j
@Component
public class OrderNumberGenerator {

    private static final String ORDER_PREFIX = "ORD";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    @Value("${app.instance.id:001}")
    private String instanceId;

    private final AtomicLong sequenceCounter = new AtomicLong(1);
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a professional order number with format: ORD-YYYYMMDD-HHMMSS-INSTANCE-SEQUENCE-CHECKSUM
     * Example: ORD-20241215-143022-001-000001-7A
     *
     * @return professionally formatted order number
     */
    public String generateOrderNumber(String partnerId) {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_FORMAT);
        String timePart = now.format(TIME_FORMAT);

        // Get next sequence number (resets daily automatically due to date inclusion)
        long sequence = getNextSequence();
        String sequencePart = String.format("%06d", sequence);

        // Generate custom instance ID from partner ID
        String instanceId = String.format("%03d", Integer.parseInt(partnerId));

        // Generate checksum for integrity
        String baseOrderNumber = String.format("%s-%s-%s-%s-%s",
                ORDER_PREFIX, datePart, timePart, instanceId, sequencePart);
        String checksum = generateChecksum(baseOrderNumber);

        String finalOrderNumber = baseOrderNumber + "-" + checksum;

        log.debug("Generated order number: {}", finalOrderNumber);
        return finalOrderNumber;
    }

    /**
     * Generates a simplified order number with format: ORD-YYYYMMDD-SEQUENCE
     * Example: ORD-20241215-000001
     *
     * @return simplified professional order number
     */
    public String generateSimpleOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_FORMAT);
        long sequence = getNextSequence();
        String sequencePart = String.format("%06d", sequence);

        String orderNumber = String.format("%s-%s-%s", ORDER_PREFIX, datePart, sequencePart);

        log.debug("Generated simple order number: {}", orderNumber);
        return orderNumber;
    }

    /**
     * Generates a compact order number with format: ORDYYYYMMDDHHMMSSSEQ
     * Example: ORD20241215143022001
     *
     * @return compact professional order number
     */
    public String generateCompactOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_FORMAT);
        String timePart = now.format(TIME_FORMAT);
        long sequence = getNextSequence() % 1000; // Keep last 3 digits only for compactness
        String sequencePart = String.format("%03d", sequence);

        String orderNumber = ORDER_PREFIX + datePart + timePart + sequencePart;

        log.debug("Generated compact order number: {}", orderNumber);
        return orderNumber;
    }

    /**
     * Generates order number with custom prefix
     *
     * @param prefix custom prefix to use instead of "ORD"
     * @return order number with custom prefix
     */
    public String generateOrderNumberWithPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = ORDER_PREFIX;
        }

        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_FORMAT);
        String timePart = now.format(TIME_FORMAT);
        long sequence = getNextSequence();
        String sequencePart = String.format("%06d", sequence);
        String checksum = generateChecksum(prefix + datePart + timePart + sequencePart);

        String orderNumber = String.format("%s-%s-%s-%s-%s",
                prefix.toUpperCase(), datePart, timePart, sequencePart, checksum);

        log.debug("Generated order number with prefix '{}': {}", prefix, orderNumber);
        return orderNumber;
    }

    /**
     * Validates if an order number follows the expected format
     *
     * @param orderNumber the order number to validate
     * @return true if valid format, false otherwise
     */
    public boolean isValidOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return false;
        }

        // Check full format: ORD-YYYYMMDD-HHMMSS-INSTANCE-SEQUENCE-CHECKSUM
        String fullPattern = "^[A-Z]{3}-\\d{8}-\\d{6}-\\d{3}-\\d{6}-[A-Z0-9]{2}$";

        // Check simple format: ORD-YYYYMMDD-SEQUENCE
        String simplePattern = "^[A-Z]{3}-\\d{8}-\\d{6}$";

        // Check compact format: ORDYYYYMMDDHHMMSSSEQ
        String compactPattern = "^[A-Z]{3}\\d{8}\\d{6}\\d{3}$";

        return orderNumber.matches(fullPattern) ||
                orderNumber.matches(simplePattern) ||
                orderNumber.matches(compactPattern);
    }

    /**
     * Extracts the date from an order number
     *
     * @param orderNumber the order number
     * @return LocalDateTime if extractable, null otherwise
     */
    public LocalDateTime extractDateFromOrderNumber(String orderNumber) {
        try {
            if (orderNumber.contains("-")) {
                // Handle hyphenated formats
                String[] parts = orderNumber.split("-");
                if (parts.length >= 2) {
                    String datePart = parts[1];
                    if (parts.length >= 3) {
                        String timePart = parts[2];
                        return LocalDateTime.parse(datePart + timePart,
                                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    } else {
                        return LocalDateTime.parse(datePart + "000000",
                                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    }
                }
            } else if (orderNumber.length() >= 11) {
                // Handle compact format
                String datePart = orderNumber.substring(3, 11); // Skip prefix, get YYYYMMDD
                String timePart = orderNumber.length() >= 17 ?
                        orderNumber.substring(11, 17) : "000000"; // Get HHMMSS or default
                return LocalDateTime.parse(datePart + timePart,
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
        } catch (Exception e) {
            log.warn("Failed to extract date from order number: {}", orderNumber, e);
        }
        return null;
    }

    private long getNextSequence() {
        return sequenceCounter.getAndIncrement();
    }

    private String generateChecksum(String input) {
        // Simple checksum using CRC-like algorithm
        int checksum = 0;
        for (char c : input.toCharArray()) {
            checksum = (checksum * 31 + c) % 1296; // 36^2 for base36 two-char result
        }
        return String.format("%02X", checksum % 256);
    }

    /**
     * Resets the sequence counter (useful for testing or daily resets)
     */
    public void resetSequence() {
        sequenceCounter.set(1);
        log.info("Order number sequence counter reset");
    }

    /**
     * Gets current sequence number without incrementing
     *
     * @return current sequence number
     */
    public long getCurrentSequence() {
        return sequenceCounter.get();
    }
}
