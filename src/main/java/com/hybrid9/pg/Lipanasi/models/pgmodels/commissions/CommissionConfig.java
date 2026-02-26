package com.hybrid9.pg.Lipanasi.models.pgmodels.commissions;

import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannelConfig;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 10091320L;
   // private String vendorId;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal baseFee;
    private BigDecimal percentageRate;
    private String commissionTireId;
    private PaymentMethod paymentMethod;
    //private MobileMoneyChannelConfig mobileMoneyChannelConfig;
    private String paymentMethodName;
    private String paymentMethodChanelName;
    private CommissionStatus commissionStatus;
    // Payment Method Related
    private Map<String, Object> paymentMethodConfig;

    // Payment Channel Related
    private Map<String, Object> paymentChannelConfig;


    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;


    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public enum CommissionStatus {
        ACTIVE,INACTIVE;

        public static CommissionStatus getTransactionStatus(CommissionConfig commissionConfig) {
            return commissionConfig.commissionStatus;
        }
    }

}
