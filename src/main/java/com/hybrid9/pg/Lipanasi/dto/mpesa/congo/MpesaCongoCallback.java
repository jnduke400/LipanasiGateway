package com.hybrid9.pg.Lipanasi.dto.mpesa.congo;


import lombok.Data;

@Data
public class MpesaCongoCallback {
    private String resultType;
    private String resultCode;
    private String resultDesc;
    private String originatorConversationId;
    private String conversationId;
    private String thirdPartyReference;
    private String amount;
    private String transactionTime;
    private String insightReference;
    private String transactionId;
}
