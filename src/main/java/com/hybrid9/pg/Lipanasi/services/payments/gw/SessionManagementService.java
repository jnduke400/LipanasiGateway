package com.hybrid9.pg.Lipanasi.services.payments.gw;

import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SessionManagementService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SESSION_PREFIX = "scoop-pg-session:";
    private static final String USER_SESSIONS_PREFIX = "scoop-pg-session-index:";
    @Value("${order.session.expiry.default:30}")
    private int DEFAULT_SESSION_EXPIRY; // minutes

    public SessionManagementService(RedisTemplate<String, Object> redisTemplate,
                                    StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Create a new session
     */
    public String createSession(UserSession session) {
        String sessionId = generateSessionId();
        String key = SESSION_PREFIX + sessionId;

        // Set creation time if not already set
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(LocalDateTime.now());
        }
        session.setLastAccessedAt(LocalDateTime.now());

        // Store the session
        redisTemplate.opsForValue().set(key, session);
        redisTemplate.expire(key, DEFAULT_SESSION_EXPIRY, TimeUnit.MINUTES);

        // Create index by user ID for easy lookup
        if (session.getUserId() != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
            stringRedisTemplate.opsForSet().add(userSessionsKey, sessionId);
            stringRedisTemplate.expire(userSessionsKey, DEFAULT_SESSION_EXPIRY, TimeUnit.MINUTES);
        }

        log.debug("Created new session: {} for user: {}", sessionId, session.getUserId());
        return sessionId;
    }

    /**
     * Retrieve session by ID
     */
    public Optional<UserSession> getSession(String sessionId) {
        try {
            String key = SESSION_PREFIX + sessionId;
            UserSession session = (UserSession) redisTemplate.opsForValue().get(key);

            if (session != null) {
                // Update last accessed time
                session.updateLastAccessed();
                redisTemplate.opsForValue().set(key, session);
                // Refresh expiry
                redisTemplate.expire(key, DEFAULT_SESSION_EXPIRY, TimeUnit.MINUTES);

                return Optional.of(session);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving session: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Update an existing session
     */
    public void updateSession(String sessionId, UserSession session) {
        String key = SESSION_PREFIX + sessionId;
        session.updateLastAccessed();
        redisTemplate.opsForValue().set(key, session);
        redisTemplate.expire(key, DEFAULT_SESSION_EXPIRY, TimeUnit.MINUTES);
        log.debug("Updated session: {}", sessionId);
    }

    /**
     * Update a specific attribute in the session
     */
    public void updateSessionAttribute(String sessionId, String attributeKey, Object attributeValue) {
        getSession(sessionId).ifPresent(session -> {
            session.addAttribute(attributeKey, attributeValue);
            updateSession(sessionId, session);
        });
    }

    /**
     * Invalidate a session
     */
    public void invalidateSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;

        // Get the session first to remove from user index
        UserSession session = (UserSession) redisTemplate.opsForValue().get(key);
        if (session != null && session.getUserId() != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
            stringRedisTemplate.opsForSet().remove(userSessionsKey, sessionId);
        }

        // Delete the session
        redisTemplate.delete(key);
        log.debug("Invalidated session: {}", sessionId);
    }

    /**
     * Find all sessions for a user
     */
    public List<UserSession> findSessionsByUserId(String userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(userSessionsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSession> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            getSession(sessionId).ifPresent(sessions::add);
        }

        return sessions;
    }

    /**
     * Invalidate all sessions for a user
     */
    public void invalidateUserSessions(String userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(userSessionsKey);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            for (String sessionId : sessionIds) {
                redisTemplate.delete(SESSION_PREFIX + sessionId);
            }
        }

        // Remove the index
        stringRedisTemplate.delete(userSessionsKey);
        log.debug("Invalidated all sessions for user: {}", userId);
    }

    /**
     * Update transaction state in session
     */
    public void updateTransactionState(String sessionId, String transactionId,
                                       UserSession.TransactionStatus status) {
        getSession(sessionId).ifPresent(session -> {
            session.setCurrentTransactionId(transactionId);
            session.setTransactionStatus(status);
            updateSession(sessionId, session);
        });
    }

    /**
     * Extend session expiry time
     */
    public void extendSessionExpiry(String sessionId, int minutes) {
        String key = SESSION_PREFIX + sessionId;
        if (redisTemplate.hasKey(key)) {
            redisTemplate.expire(key, minutes, TimeUnit.MINUTES);

            // Also update the user index expiry
            UserSession session = (UserSession) redisTemplate.opsForValue().get(key);
            if (session != null && session.getUserId() != null) {
                String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
                stringRedisTemplate.expire(userSessionsKey, minutes, TimeUnit.MINUTES);
            }

            log.debug("Extended session expiry for session: {} to {} minutes", sessionId, minutes);
        }
    }

    /**
     * Find sessions by merchant ID
     */
    public List<UserSession> findSessionsByMerchantId(String merchantId) {
        Set<String> sessionKeys = redisTemplate.keys(SESSION_PREFIX + "*");
        if (sessionKeys == null || sessionKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSession> matchingSessions = new ArrayList<>();
        for (String key : sessionKeys) {
            UserSession session = (UserSession) redisTemplate.opsForValue().get(key);
            if (session != null && merchantId.equals(session.getMerchantId())) {
                matchingSessions.add(session);
            }
        }

        return matchingSessions;
    }

    /**
     * Find all active sessions for a specific user
     * @param userId The user ID to search for
     * @param includeExpired Whether to include potentially expired sessions
     * @return List of user sessions
     */
    public List<UserSession> getSessionsByUserId(String userId, boolean includeExpired) {
        if (userId == null || userId.isEmpty()) {
            log.warn("Attempted to retrieve sessions with null or empty userId");
            return Collections.emptyList();
        }

        log.debug("Retrieving sessions for user: {}", userId);
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;

        // Get all session IDs for this user
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(userSessionsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("No sessions found for user: {}", userId);
            return Collections.emptyList();
        }

        List<UserSession> sessions = new ArrayList<>();
        List<String> invalidSessionIds = new ArrayList<>();

        // Retrieve each session
        for (String sessionId : sessionIds) {
            String key = SESSION_PREFIX + sessionId;

            // Check if the session key exists before retrieving
            if (!includeExpired && !Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                invalidSessionIds.add(sessionId);
                continue;
            }

            try {
                UserSession session = (UserSession) redisTemplate.opsForValue().get(key);
                if (session != null) {
                    sessions.add(session);
                } else {
                    invalidSessionIds.add(sessionId);
                }
            } catch (Exception e) {
                log.error("Error retrieving session: {} for user: {}", sessionId, userId, e);
                invalidSessionIds.add(sessionId);
            }
        }

        // Clean up invalid session references if any were found
        if (!invalidSessionIds.isEmpty() && !includeExpired) {
            cleanupInvalidSessionReferences(userId, invalidSessionIds);
        }

        // Sort sessions by last accessed time (most recent first)
        sessions.sort((s1, s2) -> s2.getLastAccessedAt().compareTo(s1.getLastAccessedAt()));

        log.debug("Retrieved {} active sessions for user: {}", sessions.size(), userId);
        return sessions;
    }

    /**
     * Find a specific session for a user by device info or IP address
     * @param userId The user ID
     * @param deviceInfo Device info to filter by (can be null)
     * @param ipAddress IP address to filter by (can be null)
     * @return Optional containing the matching session if found
     */
    public Optional<UserSession> getUserSessionByDevice(String userId, String deviceInfo, String ipAddress) {
        List<UserSession> userSessions = getSessionsByUserId(userId, false);

        // Return if no sessions found
        if (userSessions.isEmpty()) {
            return Optional.empty();
        }

        // If both deviceInfo and ipAddress are null, return most recent session
        if (deviceInfo == null && ipAddress == null) {
            return Optional.of(userSessions.getFirst());
        }

        // Find session matching device info and/or IP address
        for (UserSession session : userSessions) {
            boolean deviceMatch = deviceInfo == null || deviceInfo.equals(session.getDeviceInfo());
            boolean ipMatch = ipAddress == null || ipAddress.equals(session.getIpAddress());

            if (deviceMatch && ipMatch) {
                return Optional.of(session);
            }
        }

        return Optional.empty();
    }

    /**
     * Find all sessions for a user with a specific transaction status
     * @param userId The user ID
     * @param status The transaction status to filter by
     * @return List of matching user sessions
     */
    public List<UserSession> getSessionsByTransactionStatus(String userId, UserSession.TransactionStatus status) {
        List<UserSession> userSessions = getSessionsByUserId(userId, false);

        if (status == null) {
            return userSessions;
        }

        return userSessions.stream()
                .filter(session -> status.equals(session.getTransactionStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get statistics about a user's sessions
     * @param userId The user ID
     * @return Map containing session statistics
     */
    public Map<String, Object> getUserSessionStats(String userId) {
        List<UserSession> sessions = getSessionsByUserId(userId, true);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", sessions.size());

        if (!sessions.isEmpty()) {
            // Get most recent session
            UserSession latestSession = sessions.get(0);
            stats.put("lastActivity", latestSession.getLastAccessedAt());
            stats.put("lastDeviceInfo", latestSession.getDeviceInfo());
            stats.put("lastIpAddress", latestSession.getIpAddress());

            // Count sessions by transaction status
            Map<UserSession.TransactionStatus, Long> statusCounts = sessions.stream()
                    .filter(s -> s.getTransactionStatus() != null)
                    .collect(Collectors.groupingBy(
                            UserSession.TransactionStatus::getTransactionStatus,
                            Collectors.counting()
                    ));
            stats.put("transactionStatusCounts", statusCounts);

            // Count sessions by device info
            Map<String, Long> deviceCounts = sessions.stream()
                    .filter(s -> s.getDeviceInfo() != null)
                    .collect(Collectors.groupingBy(
                            UserSession::getDeviceInfo,
                            Collectors.counting()
                    ));
            stats.put("deviceCounts", deviceCounts);
        }

        return stats;
    }

    /**
     * Clean up invalid session references from the user's session index
     * @param userId The user ID
     * @param invalidSessionIds List of invalid session IDs to remove
     */
    private void cleanupInvalidSessionReferences(String userId, List<String> invalidSessionIds) {
        if (invalidSessionIds.isEmpty()) {
            return;
        }

        String userSessionsKey = USER_SESSIONS_PREFIX + userId;

        try {
            // Remove invalid session IDs from the user's session index
            String[] sessionIdsArray = invalidSessionIds.toArray(new String[0]);
            stringRedisTemplate.opsForSet().remove(userSessionsKey, (Object[]) sessionIdsArray);

            log.debug("Cleaned up {} invalid session references for user: {}",
                    invalidSessionIds.size(), userId);
        } catch (Exception e) {
            log.error("Error cleaning up invalid session references for user: {}", userId, e);
        }
    }

    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void cleanupExpiredSessions() {
        log.debug("Running scheduled cleanup of expired sessions");

        // Find all user session index keys
        Set<String> userSessionKeys = stringRedisTemplate.keys(USER_SESSIONS_PREFIX + "*");
        if (userSessionKeys == null || userSessionKeys.isEmpty()) {
            return;
        }

        for (String userSessionKey : userSessionKeys) {
            Set<String> sessionIds = stringRedisTemplate.opsForSet().members(userSessionKey);
            if (sessionIds == null || sessionIds.isEmpty()) {
                // Delete empty user session indices
                stringRedisTemplate.delete(userSessionKey);
                continue;
            }

            // Check if any sessions in the index don't exist anymore
            for (String sessionId : sessionIds) {
                if (!redisTemplate.hasKey(SESSION_PREFIX + sessionId)) {
                    stringRedisTemplate.opsForSet().remove(userSessionKey, sessionId);
                }
            }

            // Check if index is now empty after cleanup
            if (stringRedisTemplate.opsForSet().size(userSessionKey) == 0) {
                stringRedisTemplate.delete(userSessionKey);
            }
        }
    }

    /**
     * Generate a secure session ID
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Update commission configuration values in the user session
     * @param sessionId The session ID
     * @param paymentMethodName The payment method name to update
     * @param paymentMethod The PaymentMethod object to update
     * @param paymentMethodChannelName The payment method channel name to update
     * @return true if update was successful, false if session not found or commission config is null
     */
    public boolean updateCommissionConfig(String sessionId, String paymentMethodName,
                                          PaymentMethod paymentMethod, String paymentMethodChannelName) {
        Optional<UserSession> sessionOpt = getSession(sessionId);

        if (sessionOpt.isEmpty()) {
            log.warn("Session not found for sessionId: {}", sessionId);
            return false;
        }

        UserSession session = sessionOpt.get();
        CommissionConfig commissionConfig = session.getCommissionConfig();

        if (commissionConfig == null) {
            log.warn("Commission config is null for session: {}", sessionId);
            return false;
        }

        // Update the commission config values
        if (paymentMethodName != null) {
            commissionConfig.setPaymentMethodName(paymentMethodName);
        }

        if (paymentMethod != null) {
            commissionConfig.setPaymentMethod(paymentMethod);
        }

        if (paymentMethodChannelName != null) {
            commissionConfig.setPaymentMethodChanelName(paymentMethodChannelName);
        }

        // Update last accessed time for commission config
        commissionConfig.updateLastAccessed();

        // Save the updated session
        updateSession(sessionId, session);

        log.debug("Updated commission config for session: {} with paymentMethodName: {}, paymentMethodChannelName: {}",
                sessionId, paymentMethodName, paymentMethodChannelName);

        return true;
    }

    /**
     * Update individual commission configuration field
     * @param sessionId The session ID
     * @param fieldName The field name to update ("paymentMethodName", "paymentMethod", or "paymentMethodChannelName")
     * @param fieldValue The new value for the field
     * @return true if update was successful, false otherwise
     */
    public boolean updateCommissionConfigField(String sessionId, String fieldName, Object fieldValue) {
        Optional<UserSession> sessionOpt = getSession(sessionId);

        if (sessionOpt.isEmpty()) {
            log.warn("Session not found for sessionId: {}", sessionId);
            return false;
        }

        UserSession session = sessionOpt.get();
        CommissionConfig commissionConfig = session.getCommissionConfig();

        if (commissionConfig == null) {
            log.warn("Commission config is null for session: {}", sessionId);
            return false;
        }

        try {
            switch (fieldName.toLowerCase()) {
                case "paymentmethodname":
                    commissionConfig.setPaymentMethodName((String) fieldValue);
                    break;
                case "paymentmethod":
                    commissionConfig.setPaymentMethod((PaymentMethod) fieldValue);
                    break;
                case "paymentmethodchannelname":
                case "paymentmethodchanelname": // Handle the typo in the original field name
                    commissionConfig.setPaymentMethodChanelName((String) fieldValue);
                    break;
                default:
                    log.warn("Unknown commission config field: {}", fieldName);
                    return false;
            }

            // Update last accessed time for commission config
            commissionConfig.updateLastAccessed();

            // Save the updated session
            updateSession(sessionId, session);

            log.debug("Updated commission config field '{}' for session: {}", fieldName, sessionId);
            return true;

        } catch (ClassCastException e) {
            log.error("Invalid field value type for field '{}' in session: {}", fieldName, sessionId, e);
            return false;
        }
    }

    /**
     * Get commission configuration from session
     * @param sessionId The session ID
     * @return Optional containing the CommissionConfig if found
     */
    public Optional<CommissionConfig> getCommissionConfig(String sessionId) {
        Optional<UserSession> sessionOpt = getSession(sessionId);

        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        CommissionConfig commissionConfig = sessionOpt.get().getCommissionConfig();
        return Optional.ofNullable(commissionConfig);
    }

    /**
     * Update transaction state and error message in session
     * @param sessionId The session ID
     * @param transactionId The transaction ID (can be null to keep existing)
     * @param status The transaction status
     * @param errorMessage The error message (can be null to clear existing error)
     */
    public void updateTransactionStateWithError(String sessionId, String transactionId,
                                                UserSession.TransactionStatus status, String errorMessage) {
        getSession(sessionId).ifPresent(session -> {
            if (transactionId != null) {
                session.setCurrentTransactionId(transactionId);
            }
            session.setTransactionStatus(status);
            session.setErrorMessage(errorMessage);
            updateSession(sessionId, session);

            log.debug("Updated transaction state for session: {} with status: {} and error: {}",
                    sessionId, status, errorMessage);
        });
    }

    /**
     * Update only transaction status and error message (without changing transaction ID)
     * @param sessionId The session ID
     * @param status The transaction status
     * @param errorMessage The error message (can be null to clear existing error)
     */
    public void updateTransactionStatusAndError(String sessionId,
                                                UserSession.TransactionStatus status, String errorMessage) {
        updateTransactionStateWithError(sessionId, null, status, errorMessage);
    }

    /**
     * Update only error message in session
     * @param sessionId The session ID
     * @param errorMessage The error message (can be null to clear existing error)
     */
    public void updateTransactionError(String sessionId, String errorMessage) {
        getSession(sessionId).ifPresent(session -> {
            session.setErrorMessage(errorMessage);
            updateSession(sessionId, session);

            log.debug("Updated error message for session: {} with error: {}", sessionId, errorMessage);
        });
    }

    /**
     * Clear error message from session
     * @param sessionId The session ID
     */
    public void clearTransactionError(String sessionId) {
        updateTransactionError(sessionId, null);
    }
}
