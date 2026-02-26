package com.hybrid9.pg.Lipanasi.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingInformationDTO {
    private String firstName;
    private String lastName;
    private String address1;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String email;
    private String phone;
}

