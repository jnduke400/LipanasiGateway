package com.hybrid9.pg.Lipanasi.dto.auth;

import com.hybrid9.pg.Lipanasi.dto.AddressDto;
import com.hybrid9.pg.Lipanasi.dto.BusinessDto;
import com.hybrid9.pg.Lipanasi.dto.VendorAccountDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupDTO {
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
    private AddressDto address;
}
