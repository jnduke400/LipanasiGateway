package com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.camel.Pattern;
import software.amazon.awssdk.annotations.NotNull;

import java.math.BigDecimal;

@XmlRootElement(name = "COMMAND")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncBillPayRequest {
    @XmlElement(name = "TYPE")
    private String type;

    @XmlElement(name = "TXNID")
    private String txnId;

    @XmlElement(name = "MSISDN")
    private String msisdn;

    @XmlElement(name = "AMOUNT")
    private Double amount;

    @XmlElement(name = "COMPANYNAME")
    private String companyName;

    @XmlElement(name = "CUSTOMERREFERENCEID")
    private String customerReferenceId;

    @XmlElement(name = "SENDERNAME")
    private String senderName;
}
