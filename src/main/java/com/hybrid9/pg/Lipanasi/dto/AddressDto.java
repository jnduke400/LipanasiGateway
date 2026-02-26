package com.hybrid9.pg.Lipanasi.dto;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link com.gtl.mbet.paymentgateway.models.vendorx.Address}
 */
@Builder
@Value
public class AddressDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 184655362L;
    String street;
    String city;
    String state;
    String zip;
    String country;
    String houseNumber;
}