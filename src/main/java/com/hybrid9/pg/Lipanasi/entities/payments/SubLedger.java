package com.hybrid9.pg.Lipanasi.entities.payments;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.RequestType;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_sub_ledgers")
public class SubLedger extends Auditable<String> {
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
    private float prevAmount;
    private float amount;
    private float curAmount;
    private String receipt;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestType requestType = RequestType.CASH_IN;
    private String currency;
}
