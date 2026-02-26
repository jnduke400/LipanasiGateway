package com.hybrid9.pg.Lipanasi.models.session;

import com.hybrid9.pg.Lipanasi.enums.MobileNetworkType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MobileNetworkConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;
    private String merchantId;
    private String username;
    private List<String> roles;
    private Map<String, Object> attributes;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private String deviceInfo;
    private String ipAddress;

    // Payment-specific session data
    private String currentTransactionId;
    private String paymentIntentId;
    private TransactionStatus transactionStatus;
    private String errorMessage;
    private boolean authenticated;
    private boolean mfaVerified;

    // Mobile network configuration for payment
    private MobileNetworkType selectedNetworkType;
    private MobileNetworkConfig networkConfig;

    // Commission and Payment Method Related
    private CommissionConfig commissionConfig;

    //Order related
    private String orderNumber;
    private String receiptNumber;

    public enum TransactionStatus {
        INITIATED,PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED;

        public static TransactionStatus getTransactionStatus(UserSession userSession) {
            return userSession.transactionStatus;
        }
    }

    // Helper methods
    public void addAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
