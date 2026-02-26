package com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Transaction {
    private BigDecimal amount;
    private String commandID;
    private String initiator;
    private String originatorConversationID;
    private String recipient;
    private String mpesaReceipt;
    private String transactionDate;
    private String accountReference;
    private String transactionID;
    private String conversationID;
    private String resultType;
    private String resultCode;
    private String resultDesc;
    private String serviceReceipt;
    private String serviceDate;
    private String initiatorPassword;
}
