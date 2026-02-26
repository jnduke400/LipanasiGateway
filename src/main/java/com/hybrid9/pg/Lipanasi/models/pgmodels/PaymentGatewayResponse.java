package com.hybrid9.pg.Lipanasi.models.pgmodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResponse {
    private String gatewayTransactionId;
    private String status;
    private String responseCode;
    private String message;

    // 3DS fields
    private boolean requires3dsChallenge;
    private String threeDsUrl;
    private String threeDsPayload;
}
