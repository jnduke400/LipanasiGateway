package com.hybrid9.pg.Lipanasi.entities.payments.commission;

import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "c2b_commission_transactions",indexes ={
        @Index(name = "idx_commission_transaction_reference", columnList = "transaction_reference"),
        @Index(name = "idx_commission_transaction_vendor", columnList = "vendor_id"),
        @Index(name = "idx_commission_transaction_payment_method", columnList = "payment_method_id"),
        @Index(name = "idx_commission_transaction_mobile_operator", columnList = "mobile_operator_id"),
        @Index(name = "idx_commission_transaction_mobile_money_channel", columnList = "mobile_money_channel_id"),
        @Index(name = "idx_commission_transaction_card_payment", columnList = "card_payment_id"),
        @Index(name = "idx_commission_transaction_push_ussd", columnList = "push_ussd_id"),
        @Index(name = "idx_commission_transaction_pay_bill_payment", columnList = "pay_bill_payment_id")})
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTransaction extends Auditable<String> {

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @ManyToOne()
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorDetails vendor;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false)
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    private BigDecimal commissionAmount;

    @Column(nullable = false)
    private BigDecimal baseFee;

    @Column(nullable = false)
    private BigDecimal percentageFee;

    @ManyToOne()
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne()
    @JoinColumn(name = "mobile_operator_id")
    private MnoMapping mobileOperator;

    @ManyToOne()
    @JoinColumn(name = "mobile_money_channel_id")
    private MobileMoneyChannel mobileMoneyChannel;

    @ManyToOne
    @JoinColumn(name = "card_payment_id")
    private CardPayment cardPayment;

    @ManyToOne
    @JoinColumn(name = "push_ussd_id")
    private PushUssd pushUssd;

    @ManyToOne
    @JoinColumn(name = "pay_bill_payment_id")
    private PayBillPayment payBillPayment;
}
