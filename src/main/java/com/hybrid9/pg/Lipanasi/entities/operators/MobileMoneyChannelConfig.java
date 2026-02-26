package com.hybrid9.pg.Lipanasi.entities.operators;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CommissionStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "c2b_mobile_money_channel_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyChannelConfig extends Auditable<String> {

    @ManyToOne()
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorDetails vendor;

    @ManyToOne()
    @JoinColumn(name = "mobile_operator_id", nullable = false)
    private MnoMapping mobileOperator;

    @ManyToOne()
    @JoinColumn(name = "mobile_money_channel_id", nullable = false)
    private MobileMoneyChannel mobileMoneyChannel;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CommissionStatus status = CommissionStatus.ACTIVE;

}
