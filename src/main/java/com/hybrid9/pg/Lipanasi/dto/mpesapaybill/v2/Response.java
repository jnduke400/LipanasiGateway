package com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Response {
    private String conversationID;
    private String originatorConversationID;
    private String responseCode;
    private String responseDesc;
    private String serviceStatus;
    private String transactionID;
}
