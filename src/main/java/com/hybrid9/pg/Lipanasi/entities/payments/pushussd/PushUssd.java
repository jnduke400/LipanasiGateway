package com.hybrid9.pg.Lipanasi.entities.payments.pushussd;

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
@Table(name = "c2b_push_ussd",indexes = {@Index(name = "push_ussd_msisdn", columnList = "msisdn"),@Index(name = "push_ussd_reference", columnList = "reference"),@Index(name = "push_ussd_operator", columnList = "operator"),@Index(name="push_session_id", columnList = "session_id")})
public class PushUssd extends Auditable<String> {
    @Column(name = "is_success")
    private boolean isSuccess;
    private String message;
    @Column(name = "gateway_id")
    private String gatewayId;
    private String reference;
    @Column(name = "account_id")
    private String accountId;
    private String transactionNumber;
    private String msisdn;
    private String currency;
    private float amount;
    private String status;
    private String nonce;
    private String details;
    private String operator;
    private String receiptNumber;
    @Column(name = "session_id")
    private String sessionId;
    private String tqsTransactionId;
    private String refundRequestId;
    private String refundRequestStatus;
    private String refundTransactionId;
    private String lastQueryDate;
    @Builder.Default
    @Column(name = "query_attempts", columnDefinition = "INT DEFAULT 0")
    private int queryAttempts = 0;
    @Builder.Default
    @Column(name = "collection_type")
    private String collectionType = "PUSH USSD";
    @Column(name = "error_message")
    private String errorMessage;
    private String event;
    @Column(name = "billing_page_url")
    private String billingPageUrl;
    @Column(name = "cancel_billing_url")
    private String cancelBillingUrl;
    @Column(name = "expires_at")
    private String expiresAt;
    @Builder.Default
    @Column(name = "collection_status")
    @Enumerated(EnumType.STRING)
    private CollectionStatus collectionStatus = CollectionStatus.NEW;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
}
