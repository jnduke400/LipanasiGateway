package com.hybrid9.pg.Lipanasi.models.pgmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreeDsVerificationRequest {
    private String gatewayTransactionId;
    private String paRes;  // Payment Authentication Response
    private String md;     // Merchant Data
}
