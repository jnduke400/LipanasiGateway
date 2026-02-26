package com.hybrid9.pg.Lipanasi.component.halopesa;

// Response class
public class Response {
    private String transactionNumber;
    private Long gatewayId;
    private String responseCode;
    private String responseStatus;
    private String reference;

    // Getters and Setters
    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public Long getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(Long gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    // Utility method to check if response is successful
    public boolean isSuccessful() {
        return "0".equals(responseCode);
    }
}