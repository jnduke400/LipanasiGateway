package com.hybrid9.pg.Lipanasi.dto.orderpayment;

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
public class PaymentRequest {
    private String orderNumber;
    private String paymentChannel;
    private String paymentMethod;
    private String msisdn;
    // Bank Payment Related
    private String transientToken;
    private String billingInfo;
}
