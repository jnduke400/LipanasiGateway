package com.hybrid9.pg.Lipanasi.entities.payments.activity;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_failed_deposits",indexes = {@Index(name = "failed_deposit_msisdn", columnList = "msisdn"),@Index(name = "failed_deposit_reference", columnList = "reference"),@Index(name ="failed_deposit_push_ussd", columnList = "push_ussd_id"),@Index(name = "failed_deposit_card_payment", columnList = "card_payment_id")})
public class FailedDeposits extends Auditable<String> {
    private String msisdn;
    private String reference;
    private String originalReference;
    private String errorMessage;
    @ManyToOne
    @JoinColumn(name = "push_ussd_id")
    private PushUssd pushUssd;
    @ManyToOne
    @JoinColumn(name = "card_payment_id")
    private CardPayment cardPayment;
}
