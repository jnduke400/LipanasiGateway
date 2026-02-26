package com.hybrid9.pg.Lipanasi.dto.payscoopconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MerchantCharge {
    @JsonProperty("ID")
    private Long id;

    @JsonProperty("CreatedAt")
    private String createdAt;

    @JsonProperty("UpdatedAt")
    private String updatedAt;

    @JsonProperty("DeletedAt")
    private String deletedAt;

    @JsonProperty("merchant_id")
    private Long merchantId;

    @JsonProperty("mno")
    private String mno;

    @JsonProperty("charge_type")
    private String chargeType;

    @JsonProperty("rate")
    private Double rate;

    @JsonProperty("is_active")
    private Boolean isActive;

    // Constructors
    public MerchantCharge() {}

    public MerchantCharge(Long id, String createdAt, String updatedAt, String deletedAt,
                          Long merchantId, String mno, String chargeType, Double rate, Boolean isActive) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.merchantId = merchantId;
        this.mno = mno;
        this.chargeType = chargeType;
        this.rate = rate;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMno() {
        return mno;
    }

    public void setMno(String mno) {
        this.mno = mno;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "MerchantCharge{" +
                "id=" + id +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", deletedAt='" + deletedAt + '\'' +
                ", merchantId=" + merchantId +
                ", mno='" + mno + '\'' +
                ", chargeType='" + chargeType + '\'' +
                ", rate=" + rate +
                ", isActive=" + isActive +
                '}';
    }
}
