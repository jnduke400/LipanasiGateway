package com.hybrid9.pg.Lipanasi.dto.congo.mpesa;

import lombok.Data;

@Data
public class C2BPaymentResponse {
    private String insightReference;
    private String responseCode;
    private String customerMsisdn;
    private String thirdPartyReference;
}

