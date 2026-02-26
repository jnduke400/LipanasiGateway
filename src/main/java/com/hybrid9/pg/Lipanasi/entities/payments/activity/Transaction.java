package com.hybrid9.pg.Lipanasi.entities.payments.activity;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.*;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_transactions",uniqueConstraints = {@UniqueConstraint(columnNames = {"msisdn","channel","payment_reference","order_number"})},indexes = {
        @Index(columnList = "msisdn"),
        @Index(columnList = "channel"),
        @Index(columnList = "vendor_id"),
        @Index(columnList = "original_reference"),
        @Index(columnList = "payment_method_id"),
        @Index(columnList = "order_id"),
        @Index(columnList = "customer_id"),
        @Index(columnList = "order_number"),
        @Index(columnList = "push_ussd_id"),
        @Index(columnList = "card_payment_id")

})
public class Transaction extends Auditable<String> {
    private String msisdn;
    private double amount;
    private double afterTaxAmount;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentChannel channel = PaymentChannel.PUSH_USSD;
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;
    @Column(name = "payment_reference")
    private String paymentReference;
    @Column(name="original_reference")
    private String originalReference;
    @Column(name="order_number")
    private String orderNumber;
    private String operator;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType = TransactionType.DEPOSIT;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus = RequestStatus.NEW;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;
    private String thirdPartyResponse;
    private int retryCount;
    private String currency;
    private String payBillTransactionId;
    private String completionDate;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
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
