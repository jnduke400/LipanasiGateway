package com.hybrid9.pg.Lipanasi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentChannelConfig{
    private String mobileOperator;
    private String mobileMoneyChannel;
    private String commissionStatus;
}
