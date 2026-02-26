package com.hybrid9.pg.Lipanasi.dto;

import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionStatusDTO {
    private String originalReference;
    private String operator;
    private String currency;
    private PaymentChannel channel;
    private String event;
    private CollectionStatus collectionStatus;
    private double amount;
    private String message;
    private String thirdPartyResponse;
    private String msisdn;
}
