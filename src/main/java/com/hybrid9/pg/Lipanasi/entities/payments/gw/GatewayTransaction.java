package com.hybrid9.pg.Lipanasi.entities.payments.gw;

import com.hybrid9.pg.Lipanasi.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "c2b_gateway_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayTransaction {
    @Id
    private String id;

    @Version
    private Integer version;

    private String userId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String cardToken;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String paymentGatewayId;
    private String responseCode;
    private String responseMessage;

    // Order related fields
    private String orderNumber;
    private String receiptNumber;

    // Payment related fields
    private String paymentMethod;
    private String paymentChannel;

    // 3DS specific fields
    private String threeDsUrl;
    private String threeDsPayload;
    private boolean liabilityShifted;
    private String eci; // Electronic Commerce Indicator
    private String cavv; // Cardholder Authentication Verification Value
    private String xid;  // Transaction ID from the 3DS server

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
