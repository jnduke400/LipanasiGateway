package com.hybrid9.pg.Lipanasi.dto;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for {@link com.gtl.mbet.paymentgateway.models.vendorx.Business}
 */
@Builder
@Value
public class BusinessDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1887653678L;
    String name;
    String businessType;
}