package com.hybrid9.pg.Lipanasi.entities.payments.tax;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "c2b_transaction_taxes",indexes = {@Index(name = "idx_transaction_tax_tax_ref", columnList = "tax_reference_number"),@Index(name = "idx_transaction_tax_payment_reference", columnList = "payment_reference")})
public class TransactionTax extends Auditable<String> {
    private float taxRate;
    private float taxAmount;
    @Column(name = "tax_reference_number")
    private String taxReferenceNumber;
    private boolean taxPaid;
    @Column(name = "payment_reference")
    private String paymentReference;
    @Column(name = "vfd_request",length = 1000)
    private String vfdRequest;
    private int retryCount;
    private String errorMessage;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.INITIATED;
}
