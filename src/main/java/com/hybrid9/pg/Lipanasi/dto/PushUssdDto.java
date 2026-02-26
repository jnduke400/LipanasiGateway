package com.hybrid9.pg.Lipanasi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import lombok.*;

/**
 * DTO for {@link com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushUssdDto{
    Long id;
    Integer version;
    boolean isSuccess;
    String message;
    String invoiceNo;
    String reference;
    String sessionId;
    String accountId;
    String collectionRef;
    String msisdn;
    String currency;
    float amount;
    String status;
    String nonce;
    String details;
    String operator;
    String collectionType;
    String errorMessage;
    String event;
    String billingPageUrl;
    String cancelBillingUrl;
    String expiresAt;
    VendorDto vendorDto;
    CollectionStatus collectionStatus;
}