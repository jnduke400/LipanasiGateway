package com.hybrid9.pg.Lipanasi.dto.order;

import com.hybrid9.pg.Lipanasi.dto.commission.CommissionConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorInfo {
    private String partnerId;
    private String apiKey;
    private  String status;
    private String charges;
    private String hasCommission;
    private String hasVat;
    private String vfdType;
    // Service Related Configurations
    private Boolean isAirtimeAllowed;
    private Boolean isC2bAllowed;
    private Boolean isBundleAllowed;
    private Boolean isB2cAllowed;

    private String callbackUrl;
    private CommissionConfig commissionConfig;


}
