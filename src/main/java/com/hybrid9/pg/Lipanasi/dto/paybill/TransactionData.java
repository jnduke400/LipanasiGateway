package com.hybrid9.pg.Lipanasi.dto.paybill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {
    private String transactionId;
    private String status;
    private Double amount;
}
