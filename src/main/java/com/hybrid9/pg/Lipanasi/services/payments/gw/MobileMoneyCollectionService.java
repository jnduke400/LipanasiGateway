package com.hybrid9.pg.Lipanasi.services.payments.gw;


import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MobileMoneyCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Slf4j
@Service
public class MobileMoneyCollectionService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String MOBILE_MONEY_COLLECTION_KEY = "collection:";

    /**
     * Retrieve Mobile Money collection data as JSON string
     */
    public Optional<String> getMobileMoneyCollectionAsJson(String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            String jsonData = redisTemplate.opsForValue().get(key);
            if (jsonData != null && !jsonData.isEmpty()) {
                log.info("Retrieved Mobile Money collection data from Redis");
                return Optional.of(jsonData);
            }
            log.warn("No Mobile Money collection data found in Redis");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving Mobile Money collection from Redis: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Retrieve Mobile Money collection data as a POJO
     */
    public Optional<MobileMoneyCollection> getMobileMoneyCollection(String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            String jsonData = redisTemplate.opsForValue().get(key);
            if (jsonData != null && !jsonData.isEmpty()) {
                MobileMoneyCollection collection = objectMapper.readValue(jsonData, MobileMoneyCollection.class);
                log.info("Retrieved and parsed Mobile Money collection data from Redis");
                return Optional.of(collection);
            }
            log.warn("No Mobile Money collection data found in Redis");
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.error("Error parsing Mobile Money collection JSON: {}", e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving Mobile Money collection from Redis: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Check if Mobile Money collection exists in Redis
     */
    public boolean existsMobileMoneyCollection(String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking Mobile Money collection existence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get TTL (Time To Live) for Mobile Money collection key
     */
    public long getMobileMoneyCollectionTTL(String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting TTL for Mobile Money collection: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Store/Update Mobile Money collection data
     */
    public boolean setMobileMoneyCollection(MobileMoneyCollection collection,String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            String jsonData = objectMapper.writeValueAsString(collection);
            redisTemplate.opsForValue().set(key, jsonData);
            log.info("Successfully stored Mobile Money collection data in Redis");
            return true;
        } catch (JsonProcessingException e) {
            log.error("Error serializing Mobile Money collection to JSON: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Error storing Mobile Money collection in Redis: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Store/Update Mobile Money collection data with TTL
     */
    public boolean setMobileMoneyCollection(MobileMoneyCollection collection, long timeout, TimeUnit timeUnit,String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            String jsonData = objectMapper.writeValueAsString(collection);
            redisTemplate.opsForValue().set(key, jsonData, timeout, timeUnit);
            log.info("Successfully stored Mobile Money collection data in Redis with TTL: {} {}", timeout, timeUnit);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Error serializing Mobile Money collection to JSON: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Error storing Mobile Money collection in Redis: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete Mobile Money collection from Redis
     */
    public boolean deleteMobileMoneyCollection(String collectionId) {
        try {
            String key = MOBILE_MONEY_COLLECTION_KEY + collectionId;
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Successfully deleted Mobile Money collection from Redis");
                return true;
            }
            log.warn("Mobile Money collection key not found for deletion");
            return false;
        } catch (Exception e) {
            log.error("Error deleting Mobile Money collection from Redis: {}", e.getMessage(), e);
            return false;
        }
    }
}
