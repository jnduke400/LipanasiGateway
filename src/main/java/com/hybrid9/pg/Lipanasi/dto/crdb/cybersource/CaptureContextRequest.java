package com.hybrid9.pg.Lipanasi.dto.crdb.cybersource;

public class CaptureContextRequest {
    private String[] targetOrigins;
    private String profileId;
    private String accessKey;
    private String secretKey;

    // Getters and setters
    public String[] getTargetOrigins() { return targetOrigins; }
    public void setTargetOrigins(String[] targetOrigins) { this.targetOrigins = targetOrigins; }
    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
}
