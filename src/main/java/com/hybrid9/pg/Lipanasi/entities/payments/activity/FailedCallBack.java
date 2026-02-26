package com.hybrid9.pg.Lipanasi.entities.payments.activity;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
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
@Table(name = "c2b_failed_callbacks",indexes = {@Index(name = "failed_callback_msisdn", columnList = "msisdn"),@Index(name = "failed_callback_reference", columnList = "reference"),@Index(name="failed_callback_push_ussd_id", columnList = "push_ussd_id"),@Index(name="failed_callback_card_payment_id", columnList = "card_payment_id")})
public class FailedCallBack extends Auditable<String> {
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
    @ManyToOne
    @JoinColumn(name = "pay_bill_payment_id")
    private PayBillPayment payBillPayment;
}
