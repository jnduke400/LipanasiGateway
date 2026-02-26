package com.hybrid9.pg.Lipanasi.entities.payments;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_cash_in_txns",
indexes = {@Index(name = "idx_deposit_msisdn_reference_vendor", columnList = "msisdn,payment_reference,channel,vendor_id,transaction_id")})
public class Deposit extends Auditable<String> {
    private String msisdn;
    private double amount;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentChannel channel = PaymentChannel.PUSH_USSD;
    @Column(name = "payment_reference")
    private String paymentReference;
    private String originalReference;
    @Column(name = "transaction_id")
    private String transactionId;
    private String operator;
    private String currency;
    private String sessionId;
    private String errorMessage;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus = RequestStatus.NEW;
    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorDetails;
}
