package com.hybrid9.pg.Lipanasi.dto.crdb.cybersource;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PaymentRequest {
    private String transientToken;
    private String cvv;
    private String amount;
    private String currency;
    private String orderId;
    private boolean capture;
    private BillingInfo billingInfo;

    // Getters and setters
    /*public String getTransientToken() { return transientToken; }
    public void setTransientToken(String transientToken) { this.transientToken = transientToken; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public boolean isCapture() { return capture; }
    public void setCapture(boolean capture) { this.capture = capture; }
    public BillingInfo getBillingInfo() { return billingInfo; }
    public void setBillingInfo(BillingInfo billingInfo) { this.billingInfo = billingInfo; }*/
}
