package com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement(name = "COMMAND")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class PayBillValidationRequest {
    @XmlElement(name = "TYPE")
    private String type;

    @XmlElement(name = "CUSTOMERMSISDN")
    private String customerMsisdn;

    @XmlElement(name = "MERCHANTMSISDN")
    private String merchantMsisdn;

    @XmlElement(name = "CUSTOMERNAME")
    private String customerName;

    @XmlElement(name = "AMOUNT")
    private Double amount;

    @XmlElement(name = "PIN")
    private String pin;

    @XmlElement(name = "REFERENCE")
    private String reference;

    @XmlElement(name = "USERNAME")
    private String username;

    @XmlElement(name = "PASSWORD")
    private String password;

    @XmlElement(name = "REFERENCE1")
    private String reference1;
}
