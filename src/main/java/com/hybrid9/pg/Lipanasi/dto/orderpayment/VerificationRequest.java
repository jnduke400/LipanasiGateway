package com.hybrid9.pg.Lipanasi.dto.orderpayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    private String transactionId;
    private String paRes;  // Payment Authentication Response
    private String md;     // Merchant Data
}
