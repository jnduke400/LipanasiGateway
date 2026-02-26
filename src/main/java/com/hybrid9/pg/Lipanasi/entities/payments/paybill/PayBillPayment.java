package com.hybrid9.pg.Lipanasi.entities.payments.paybill;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_pay_bill_payments",indexes = {@Index(name = "pay_bill_msisdn", columnList = "msisdn"),@Index(name = "pay_bill_reference", columnList = "payment_reference"),@Index(name = "pay_bill_operator", columnList = "operator"),@Index(name="pay_bill_vendor_id", columnList = "vendor_id"),@Index(name = "pay_bill_session_id", columnList = "session_id")})
public class PayBillPayment extends Auditable<String> {
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
    private String payBillId;
    @Column(name= "payment_reference")
    private String paymentReference;
    private float amount;
    private String currency;
    private String msisdn;
    private String accountId;
    private String operator;
    private String receiptNumber;
    private String sessionId;
    private String transactionDate;
    private String errorMessage;
    private String originalReference;
    private String validationId;
    private String validationErrorMessage;
    private String validationErrorCode;
    private String status;
    @Builder.Default
    private String collectionType = "PAY BILL";
    @Builder.Default
    @Column(name = "collection_status")
    @Enumerated(EnumType.STRING)
    private CollectionStatus collectionStatus = CollectionStatus.NEW;

}
