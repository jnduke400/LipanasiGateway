package com.hybrid9.pg.Lipanasi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import lombok.*;

/**
 * DTO for {@link com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayBillPaymentDto {
    Long id;
    Integer version;
    VendorDto vendorDto;
    String payBillId;
    String paymentReference;
    float amount;
    String currency;
    String msisdn;
    String operator;
    String transactionDate;
    String collectionType;
    String sessionId;
    String status;
    CollectionStatus collectionStatus;
}