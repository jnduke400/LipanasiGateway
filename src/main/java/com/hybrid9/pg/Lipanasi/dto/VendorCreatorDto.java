package com.hybrid9.pg.Lipanasi.dto;

import com.hybrid9.pg.Lipanasi.dto.commission.CommissionConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorCreatorDto {
    private String username;
    private String password;
    private String firstname;
    private String middlename;
    private String lastname;
    private String phoneNumber;
    private String dialCode;
    private String email;
    private BusinessDto business;
    private VendorDto vendorDto;
    private VendorAccountDto vendorAccountDto;
    private CommissionConfig commissionConfig;
    private AddressDto address;
}
