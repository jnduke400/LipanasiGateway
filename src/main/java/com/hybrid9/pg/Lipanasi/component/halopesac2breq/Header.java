package com.hybrid9.pg.Lipanasi.component.halopesac2breq;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Header {
    @JsonProperty("spId")
    private String spId;

    private String merchantCode;
    private String spPassword;
    private String timestamp;

    // Getters and Setters
    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public String getSpPassword() {
        return spPassword;
    }

    public void setSpPassword(String spPassword) {
        this.spPassword = spPassword;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
