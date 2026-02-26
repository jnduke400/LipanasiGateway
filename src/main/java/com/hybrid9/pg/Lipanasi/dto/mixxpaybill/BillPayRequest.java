package com.hybrid9.pg.Lipanasi.dto.mixxpaybill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "COMMAND")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillPayRequest {
    @XmlElement(name = "TYPE")
    @JsonProperty("type")
    private String type;

    @XmlElement(name = "TXNID")
    @JsonProperty("transactionId")
    private String transactionId;

    @XmlElement(name = "MSISDN")
    @JsonProperty("msisdn")
    private String msisdn;

    @XmlElement(name = "AMOUNT")
    @JsonProperty("amount")
    private Float amount;

    @XmlElement(name = "COMPANYNAME")
    @JsonProperty("companyName")
    private String companyName;

    @XmlElement(name = "CUSTOMERREFERENCEID")
    @JsonProperty("customerReferenceId")
    private String customerReferenceId;
}
