package com.hybrid9.pg.Lipanasi.entities.payments;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
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
@Table(name = "c2b_cash_out_txns",
        indexes = {@Index(name = "payment_reference_index", columnList = "payment_reference"),
                @Index(name = "msisdn_index", columnList = "msisdn"),@Index(name = "vendor_id_index", columnList = "vendor_id")})
public class CashOut extends Auditable<String> {
    private String msisdn;
    private double amount;
    private String channelId;
    @Column(name = "payment_reference")
    private String paymentReference;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus = RequestStatus.INITIATED;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
}
