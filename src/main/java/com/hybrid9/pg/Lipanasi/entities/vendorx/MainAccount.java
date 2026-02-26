package com.hybrid9.pg.Lipanasi.entities.vendorx;

import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_main_accounts", indexes = {@Index(name = "account_number_index", columnList = "account_number")
, @Index(name = "vendor_id_index", columnList = "vendor_id")})
public class MainAccount extends Auditable<String> {
    @Column(name = "account_number")
    private String accountNumber;
    private String accountName;
    @ManyToOne
    @JoinColumn(name = "vendor_id", unique = true)
    private VendorDetails vendorDetails;
    @Column(name = "current_amount")
    private float currentAmount;
    @Column(name = "desired_amount")
    private float desiredAmount;
    @Column(name = "withdraw_amount")
    private float withdrowAmount;
    @Builder.Default
    @Column(name = "charges")
    private float charges = 0;
    @Builder.Default
    @Column(name = "vendor_charges", columnDefinition = "DOUBLE(16,2) DEFAULT 0.00", nullable = false)
    private float vendorCharges =0;
    @Column(name = "actual_amount")
    private float actualAmount;
    private float commissionEarned;  // Total commissions earned from this vendor
    private float vatCollected;      // Total VAT collected (if vendor is taxable)
}
