package com.hybrid9.pg.Lipanasi.dto.deposit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FailedDepositRequest {
    private String status;
    private String reference;
    private String transactionNo;
    private String message;
    private String paymentSessionId;
    private String callbackUrl;
    private String channel;
}
