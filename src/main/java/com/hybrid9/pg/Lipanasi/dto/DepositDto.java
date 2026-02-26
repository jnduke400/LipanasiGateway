package com.hybrid9.pg.Lipanasi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepositDto {
    Long id;
    String msisdn;
    double amount;
    PaymentChannel channel;
    String paymentReference;
    String originalReference;
    String transactionId;
    String operator;
    String currency;
    String sessionId;
    RequestStatus requestStatus;
    VendorDto vendorDto;

    // Add created/updated timestamps if they exist in your Deposit entity
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime creationDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime lastModifiedDate;
}