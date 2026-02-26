package com.hybrid9.pg.Lipanasi.component.halotelcollection.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequestDetailsDTO {
    private String command;
    private String reference;
    private String gatewayId;
    private String receiptNumber;
    private String msisdn;
    private Double amount;
    private String network;
}
