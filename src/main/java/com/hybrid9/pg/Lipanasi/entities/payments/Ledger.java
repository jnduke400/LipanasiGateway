package com.hybrid9.pg.Lipanasi.entities.payments;

import com.hybrid9.pg.Lipanasi.enums.RequestType;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_ledgers",indexes = {
        @Index(name = "vendor_index", columnList = "vendor_id"),
})
public class Ledger extends Auditable<String> {
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
    private float prevAmount;
    private float amount;
    private float curAmount;
    @Column(nullable = false)
    private boolean hasTaxation;

    @Column
    private float vatRate;      // Only populated if hasTaxation=true

    @Column
    private float vatAmount;    // Only populated if hasTaxation=true

    @Column(nullable = false)
    private float commissionRate;

    @Column(nullable = false)
    private float commissionAmount;

    @Column(nullable = false)
    private float netAmount;    // amount after commission and VAT
    private String receipt;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestType requestType = RequestType.CASH_IN;
}
