package com.hybrid9.pg.Lipanasi.dto.mpesa;

public class TransactionResponse {

    private boolean success;
    private String code;
    private String description;
    private String detail;
    private String transactionID;
    private String thirdPartyReference;
    private String insightReference;
    private String responseCode;

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getTransactionID() { return transactionID; }
    public void setTransactionID(String transactionID) { this.transactionID = transactionID; }

    public String getThirdPartyReference() { return thirdPartyReference; }
    public void setThirdPartyReference(String thirdPartyReference) { this.thirdPartyReference = thirdPartyReference; }

    public String getInsightReference() { return insightReference; }
    public void setInsightReference(String insightReference) { this.insightReference = insightReference; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
}


