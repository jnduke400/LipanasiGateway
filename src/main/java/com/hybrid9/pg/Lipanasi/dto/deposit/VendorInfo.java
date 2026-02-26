package com.hybrid9.pg.Lipanasi.dto.deposit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorInfo {
    private String vendorId;
    private String vendorExternalId;
    private String vendorCallbackUrl;
    private String vendorName;
    private String source; // SESSION or DATABASE
}
