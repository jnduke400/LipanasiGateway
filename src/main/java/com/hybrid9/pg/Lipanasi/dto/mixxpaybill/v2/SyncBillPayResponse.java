package com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "COMMAND")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncBillPayResponse {
    @XmlElement(name = "TYPE")
    private String type = "SYNC_BILLPAY_RESPONSE";

    @XmlElement(name = "TXNID")
    private String txnId;

    @XmlElement(name = "REFID")
    private String refId;

    @XmlElement(name = "RESULT")
    private String result;

    @XmlElement(name = "ERRORCODE")
    private String errorCode;

    @XmlElement(name = "ERRORDESC")
    private String errorDesc;

    @XmlElement(name = "MSISDN")
    private String msisdn;

    @XmlElement(name = "FLAG")
    private String flag;

    @XmlElement(name = "CONTENT")
    private String content;
}
