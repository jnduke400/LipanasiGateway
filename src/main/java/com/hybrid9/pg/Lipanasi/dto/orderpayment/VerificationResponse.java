package com.hybrid9.pg.Lipanasi.dto.orderpayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private String transactionId;
    private String gatewayTransactionId;
    private boolean verified;
    private String status;
    private String message;
}
