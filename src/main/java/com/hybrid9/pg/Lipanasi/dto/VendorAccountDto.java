package com.hybrid9.pg.Lipanasi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorAccountDto {
    private String accountName;
    private String accountNumber;
}
