package com.hybrid9.pg.Lipanasi.dto.deposit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepositRequest {
    private String status;
    private String reference;
    private String transactionNo;
    private String message;
    private String paymentSessionId;
}
