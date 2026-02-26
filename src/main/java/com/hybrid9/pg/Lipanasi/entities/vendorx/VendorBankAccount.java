package com.hybrid9.pg.Lipanasi.entities.vendorx;

import com.hybrid9.pg.Lipanasi.enums.VendorBankAccountStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_vendor_bank_accounts")
public class VendorBankAccount extends Auditable<String> {
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "account_type")
    private String accountType;
    @Builder.Default
    private VendorBankAccountStatus status = VendorBankAccountStatus.ACTIVE;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
}
