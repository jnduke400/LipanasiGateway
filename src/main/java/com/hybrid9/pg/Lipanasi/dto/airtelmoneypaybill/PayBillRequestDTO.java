package com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "requestToken", namespace = "http://www.airtel.com/")
@XmlAccessorType(XmlAccessType.FIELD)
public class PayBillRequestDTO {
    @XmlElement(name = "APIusername", namespace = "http://www.airtel.com/")
    private String APIusername;

    @XmlElement(name = "APIPassword", namespace = "http://www.airtel.com/")
    private String APIPassword;

    @XmlElement(name = "transID", namespace = "http://www.airtel.com/")
    private String transID;

    @XmlElement(name = "amount", namespace = "http://www.airtel.com/")
    private float amount;

    @XmlElement(name = "referenceField", namespace = "http://www.airtel.com/")
    private String referenceField;

    @XmlElement(name = "MSISDN", namespace = "http://www.airtel.com/")
    private String MSISDN;
}
