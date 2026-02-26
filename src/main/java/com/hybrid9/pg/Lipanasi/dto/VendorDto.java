package com.hybrid9.pg.Lipanasi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hybrid9.pg.Lipanasi.dto.commission.CommissionConfig;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.VendorStatus;

import lombok.*;

/**
 * DTO for {@link VendorDetails}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorDto {
    String vendorName;
    String vendorCode;
    String billNumber;
    String hasCommission;
    String key;
    String hasVat;
    String vatType;
    // Service Related Configurations
    Boolean isAirtimeAllowed;
    Boolean isC2bAllowed;
    Boolean isBundleAllowed;
    Boolean isB2cAllowed;

    String externalId;
    String callbackUrl;
    VendorStatus status;
    CommissionConfig commissionConfig;
    float charges;
}