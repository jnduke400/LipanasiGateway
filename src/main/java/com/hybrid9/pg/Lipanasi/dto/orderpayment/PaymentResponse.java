package com.hybrid9.pg.Lipanasi.dto.orderpayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private String gatewayTransactionId;
    private String status;
    private String message;
    private String errorCode;
    private boolean successful;

    // 3DS specific fields
    private boolean requires3dsChallenge;
    private String threeDsUrl;
    private String threeDsPayload;
}
