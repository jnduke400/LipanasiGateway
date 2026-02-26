package com.hybrid9.pg.Lipanasi.entities.payments.settlemets;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.SettlementStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorBankAccount;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_settlements",indexes = {@Index(name = "idx_settlement_vendor", columnList = "vendor_id"),@Index(name = "idx_settlement_reference", columnList = "settlement_reference")})
public class Settlement extends Auditable<String> {
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
    private double amount;
    private String currency;
    private String receipt;
    private float settlementFee;
    @Column(name = "settlement_reference")
    private String settlementReference;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private SettlementStatus status = SettlementStatus.NEW;
    //private String settlementType;
    private String settlementDate;
    @ManyToOne
    @JoinColumn(name = "vendor_bank_id")
    private VendorBankAccount vendorBank;

}
