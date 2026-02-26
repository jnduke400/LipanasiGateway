package com.hybrid9.pg.Lipanasi.models.pgmodels;

import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.BillingInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayRequest {
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String cardToken;
    private String reference;
    private String description;
    private String returnUrl;
    private String customerEmail;
    private String customerIp;
    private String paymentMethod;
    private String paymentChannel;

    // Mobile Money Payment Related
    private String operator;
    private String msisdn;

    // Partner Specific Settings
    private String accountNumber;
    private String partnerCode;

    // Session Specific Settings
    private String sessionId;

    // Billing Information
    private String billingString;
}
