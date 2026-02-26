package com.hybrid9.pg.Lipanasi.dto.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardPaymentDto {
    private Long id;
    private double amount;
    private PaymentChannel channel;
    private String paymentReference;
    private String originalReference;
    private String transactionId;
    private String bankName;
    private String bankId;
    private String cardToken;
    private String currency;
    private String sessionId;
    private String collectionType;
    private String errorMessage;
    private CollectionStatus collectionStatus;
    private VendorDto vendorDto;
    private BillingInformationDTO billingInformation;
}

