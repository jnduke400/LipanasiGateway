package com.hybrid9.pg.Lipanasi.component.halotelcollection.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultDetailsDTO {
    private String receiptNumber;
    private Integer amount;
    private String date;
    private String resultCode;
    private String resultStatus;
    private String referenceNumber;
    private String transactionNumber;
    private String message;
}