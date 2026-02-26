package com.hybrid9.pg.Lipanasi.services.payments.vendorx;

import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class VendorManagementService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VENDOR_PREFIX = "vendorx:";
    private static final String VENDOR_MANAGER_PREFIX = "vendorx-manager:";
    private static final int DEFAULT_VENDOR_EXPIRY = 30; // minutes

    public VendorManagementService(RedisTemplate<String, Object> redisTemplate,
                                     StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Store vendorx details in Redis.
     *
     * @param vendorManager the vendorx manager object containing vendorx details
     */


    public String  storeVendor(VendorManager vendorManager) {
        String key = VENDOR_PREFIX + vendorManager.getVendorExternalId();

        // Set creation time if not already set
        if (vendorManager.getCreatedAt() == null) {
            vendorManager.setCreatedAt(LocalDateTime.now());
        }
        vendorManager.setLastAccessedAt(LocalDateTime.now());
        // store vendorx
        redisTemplate.opsForValue().set(key, vendorManager);
        redisTemplate.expire(key,DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);

        // Create index by vendorx ID for easy lookup
        if (vendorManager.getVendorId() != null) {
            String vendorManagerKey = VENDOR_MANAGER_PREFIX + vendorManager.getVendorId();
            stringRedisTemplate.opsForSet().add(vendorManagerKey, vendorManager.getVendorExternalId());
            stringRedisTemplate.expire(vendorManagerKey, DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);
        }

        log.info("Stored vendorx for {}: {}", vendorManager.getVendorId(), vendorManager.getVendorExternalId());
        return vendorManager.getVendorExternalId();
    }

    /**
     * Retrieve vendorx by external Id
     */
    public Optional<VendorManager> getVendor(String vendorExternalId) {
        try {
            String key = VENDOR_PREFIX + vendorExternalId;
            VendorManager vendor = (VendorManager) redisTemplate.opsForValue().get(key);

            if (vendor != null) {
                // Update last accessed time
                vendor.updateLastAccessed();
                redisTemplate.opsForValue().set(key, vendor);
                // Refresh expiry
                redisTemplate.expire(key, DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);

                return Optional.of(vendor);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving vendorx: {}", vendorExternalId, e);
            return Optional.empty();
        }
    }

    /**
     * Update an existing vendorx
     */
    public void updateVendor(String vendorExternalId, VendorManager vendor) {
        String key = VENDOR_PREFIX + vendorExternalId;
        vendor.updateLastAccessed();
        redisTemplate.opsForValue().set(key, vendor);
        redisTemplate.expire(key, DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);
        //log.debug("Updated vendorx: {}", vendorExternalId);
    }
}
