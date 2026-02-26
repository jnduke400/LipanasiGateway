package com.hybrid9.pg.Lipanasi.dto.mixxbyyas;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class BillerPaymentRequest {
    private String customerMSISDN;
    private String billerMSISDN;
    private BigDecimal amount;
    private String remarks;
    private String referenceID;
}
