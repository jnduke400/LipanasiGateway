package com.hybrid9.pg.Lipanasi.component;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ApiPayload {
    private String msisdn;
    private double amount;
    private String reference;
    private String txnStatus;
    private String responseMessage;
}
