package com.hybrid9.pg.Lipanasi.dto.lipanasiconfig;

import java.util.List;

public class MerchantChargesResponse {
    private List<MerchantCharge> data;
    private Boolean success;

    // Constructors
    public MerchantChargesResponse() {}

    public MerchantChargesResponse(List<MerchantCharge> data, Boolean success) {
        this.data = data;
        this.success = success;
    }

    // Getters and Setters
    public List<MerchantCharge> getData() {
        return data;
    }

    public void setData(List<MerchantCharge> data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "MerchantChargesResponse{" +
                "data=" + data +
                ", success=" + success +
                '}';
    }
}
