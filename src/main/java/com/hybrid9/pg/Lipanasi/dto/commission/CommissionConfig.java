package com.hybrid9.pg.Lipanasi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommissionConfig {
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal baseFee;
    private BigDecimal percentageRate;
    private String commissionStatus;
    // Payment Method Related
    private List<PaymentMethodConfig> paymentMethodConfigs;
    // Payment Channel Conf
    private List<PaymentChannelConfig> paymentChannelConfigs;
}
