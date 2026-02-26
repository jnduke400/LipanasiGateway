package com.hybrid9.pg.Lipanasi.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class VatInitialRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1000663464L;
    private String vendorExternalId;
    private String sessionId;
    private Float amount;
    private String msisdn;
    private String collectionType;
    private String paymentReference;
}
