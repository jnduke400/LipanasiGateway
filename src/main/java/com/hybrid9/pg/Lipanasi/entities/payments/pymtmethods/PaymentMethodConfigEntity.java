package com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "c2b_payment_method_configs")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodConfigEntity extends Auditable<String> {
    @ManyToOne()
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorDetails vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
