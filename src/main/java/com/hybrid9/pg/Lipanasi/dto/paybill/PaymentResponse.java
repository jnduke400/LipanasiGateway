package com.hybrid9.pg.Lipanasi.dto.paybill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private int status;
    private TransactionData data;
    private String message;
}
