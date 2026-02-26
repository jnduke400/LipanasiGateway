package com.hybrid9.pg.Lipanasi.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodConfig{
    private String paymentMethod;
    private Boolean isActive;
}
