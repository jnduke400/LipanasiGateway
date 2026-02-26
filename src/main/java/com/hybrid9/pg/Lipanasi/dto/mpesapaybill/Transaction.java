package com.hybrid9.pg.Lipanasi.dto.mpesapaybill;

import lombok.Data;

@Data
public class Transaction {
    private String amount;
    private String commandID;
    private String initiator;
    private String conversationID;
    private String originatorConversationID;
    private String recipient;
    private String mpesaReceipt;
    private String transactionDate;
    private String accountReference;
    private String transactionID;
}