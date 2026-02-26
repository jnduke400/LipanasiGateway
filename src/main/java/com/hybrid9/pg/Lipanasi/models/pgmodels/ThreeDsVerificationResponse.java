package com.hybrid9.pg.Lipanasi.models.pgmodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThreeDsVerificationResponse {
    private String gatewayTransactionId;
    private String status;
    private String responseCode;
    private String message;
    private boolean liabilityShifted;
    private String eci;  // Electronic Commerce Indicator
    private String cavv; // Cardholder Authentication Verification Value
    private String xid;  // Transaction ID for 3DS
}
