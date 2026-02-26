package com.hybrid9.pg.Lipanasi.component;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class PushResponse {
    private boolean success;
    private String message;
    private String invoice_no;
    private String reference;
    private String msisdn;
    private String currency;
    private String amount;
    private String status;
    private String nonce;
    private String billing_page_url;
    private String cancel_billing_url;
    private String expires_at;
}
