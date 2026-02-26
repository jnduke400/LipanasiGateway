package com.hybrid9.pg.Lipanasi.component.halopesa;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Header class
public class Header {
    private String spId;
    private String spPassword;
    private String timestamp;
    private String merchantCode;
    private String apiVersion;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    // Getters and Setters
    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
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

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    // Utility method to get formatted timestamp
    public LocalDateTime getParsedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.parse(timestamp, formatter);
    }
}
