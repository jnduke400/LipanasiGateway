package com.hybrid9.pg.Lipanasi.entities.payments.bank;

import com.hybrid9.pg.Lipanasi.entities.banks.Bank;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_bank_transactions",
        indexes = {
                @Index(name = "idx_bank_id", columnList = "bank_id"),
                @Index(name = "idx_payment_reference", columnList = "payment_reference"),
                @Index(name = "idx_channel", columnList = "channel"),
                @Index(name = "idx_vendor_id", columnList = "vendor_id"),
                @Index(name = "idx_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_billing_information_id", columnList = "billing_information_id"),
        })
public class CardPayment extends Auditable<String> {
    private double amount;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentChannel channel = PaymentChannel.PUSH_USSD;
    @Column(name = "payment_reference")
    private String paymentReference;
    private String originalReference;
    @Column(name = "transaction_id")
    private String transactionId;
    // Bank related response fields
    private String bankTransactionId;
    private String bankApprovalCode;
    private String bankResponseCode;
    private String bankResponseStatus;

    private String bankName;
    private String bankId;
    @Column(name = "card_token",length = 2000)
    private String cardToken;
    private String currency;
    private String sessionId;
    private String errorMessage;
    private String status;
    private String operator;
    @Builder.Default
    @Column(name = "collection_type")
    private String collectionType = "CARD";
    @Builder.Default
    @Column(name = "collection_status")
    @Enumerated(EnumType.STRING)
    private CollectionStatus collectionStatus = CollectionStatus.NEW;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;

    @ManyToOne
    @JoinColumn(name = "billing_information_id")
    private BillingInformation billingInformation;

}
