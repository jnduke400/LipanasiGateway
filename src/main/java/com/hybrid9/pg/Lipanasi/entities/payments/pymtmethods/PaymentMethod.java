package com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods;

import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_payment_methods")
public class PaymentMethod extends Auditable<String> {
    private String code;
    private String name;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type = PaymentMethodType.MOBILE_MONEY;
    private float processingFeePercentage;
    private float processingFeeFixed;
    private float minAmount;
    private float maxAmount;
    private String currency;
}
