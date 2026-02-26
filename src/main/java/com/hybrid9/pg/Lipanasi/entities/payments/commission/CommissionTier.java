package com.hybrid9.pg.Lipanasi.entities.payments.commission;

import com.hybrid9.pg.Lipanasi.dto.commission.PaymentChannelConfig;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "c2b_commission_tiers")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTier extends Auditable<String> {
    @ManyToOne()
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorDetails vendor;

    @Column(nullable = false)
    private BigDecimal minimumAmount;

    @Column(nullable = false)
    private BigDecimal maximumAmount;

    @Column(nullable = false)
    private BigDecimal baseFee;

    @Column(nullable = false)
    private BigDecimal percentageRate;

    @ManyToOne()
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne()
    @JoinColumn(name = "payment_channel_id", nullable = false)
    private MobileMoneyChannel paymentChannel;

    @ManyToOne()
    @JoinColumn(name = "operator_id", nullable = false)
    private MnoMapping operator;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
