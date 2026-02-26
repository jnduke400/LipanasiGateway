package com.hybrid9.pg.Lipanasi.services.payments.vendorx;

import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorNetworkCharges;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VendorNetworkChargesService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VENDOR_PREFIX = "scoop-pg-vendor-charges:";
    private static final String VENDOR_MANAGER_PREFIX = "scoop-pg-vendor-charges-manager:";
    private static final int DEFAULT_VENDOR_EXPIRY = 30; // minutes

    public VendorNetworkChargesService(RedisTemplate<String, Object> redisTemplate,
                                   StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Store vendor charges in Redis.
     *
     * @param vendorCharges the vendor charges manager object containing vendor charges details
     */


    public String  storeVendor(VendorNetworkCharges vendorCharges) {
        String key = VENDOR_PREFIX + vendorCharges.getMno()+"-"+vendorCharges.getMerchantId();

        // Set creation time if not already set
        if (vendorCharges.getCreatedAt() == null) {
            vendorCharges.setCreatedAt(LocalDateTime.now());
        }
        vendorCharges.setLastAccessedAt(LocalDateTime.now());
        // store vendorx
        redisTemplate.opsForValue().set(key, vendorCharges);
        redisTemplate.expire(key,DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);

        // Create index by vendor charges ID for easy lookup
        if (vendorCharges.getId() != null) {
            String vendorChargesKey = VENDOR_MANAGER_PREFIX + vendorCharges.getId();
            stringRedisTemplate.opsForSet().add(vendorChargesKey, vendorCharges.getMno()+"-"+vendorCharges.getMerchantId());
            stringRedisTemplate.expire(vendorChargesKey, DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);
        }

        log.info("Stored vendor charges for {}: {}", vendorCharges.getId(), vendorCharges.getMno()+"-"+vendorCharges.getMerchantId());
        return vendorCharges.getMno()+"-"+vendorCharges.getMerchantId();
    }

    /**
     * Retrieve vendor charges by mno and merchant id
     */
    public Optional<VendorNetworkCharges> getVendor(String vendorMnoAndMerchantId) {
        try {
            String key = VENDOR_PREFIX + vendorMnoAndMerchantId;
            VendorNetworkCharges vendor = (VendorNetworkCharges) redisTemplate.opsForValue().get(key);

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
            log.error("Error retrieving vendor charges: {}", vendorMnoAndMerchantId, e);
            return Optional.empty();
        }
    }

    /**
     * Update an existing vendor charges
     */
    public void updateVendor(String vendorMnoAndMerchantId, VendorNetworkCharges vendor) {
        String key = VENDOR_PREFIX + vendorMnoAndMerchantId;
        vendor.updateLastAccessed();
        redisTemplate.opsForValue().set(key, vendor);
        redisTemplate.expire(key, DEFAULT_VENDOR_EXPIRY, TimeUnit.MINUTES);
        //log.debug("Updated vendorx: {}", vendorExternalId);
    }
}
