package com.hybrid9.pg.Lipanasi.dto.mpesa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class USSDCallback {
    private String resultType;
    private String resultCode;
    private String resultDesc;
    private String transactionStatus;
    private String originatorConversationID;
    private String conversationID;
    private String transID;
    private String businessNumber;
    private String currency;
    private String amount;
    private String date;
    private String thirdPartyReference;
    private String insightReference;
}
