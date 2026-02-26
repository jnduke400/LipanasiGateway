package com.hybrid9.pg.Lipanasi.entities.operators;

import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "c2b_mobile_money_channels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyChannel extends Auditable<String> {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentChannel type = PaymentChannel.PUSH_USSD;

}
