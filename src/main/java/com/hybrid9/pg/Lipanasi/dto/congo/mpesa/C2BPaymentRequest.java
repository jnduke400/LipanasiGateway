package com.hybrid9.pg.Lipanasi.dto.congo.mpesa;

import lombok.Data;

@Data
public class C2BPaymentRequest {
    private String customerMsisdn;
    private String serviceProviderCode;
    private String currency;
    private String amount;
    private String date;
    private String thirdPartyReference;
    private String commandId;
    private String language;
    private String callBackChannel;
    private String callBackDestination;
    private String surname;
    private String initials;
}
