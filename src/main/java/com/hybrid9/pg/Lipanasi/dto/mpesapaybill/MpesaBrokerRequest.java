package com.hybrid9.pg.Lipanasi.dto.mpesapaybill;


import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "mpesaBroker", namespace = "http://infowise.co.tz/broker/")
public class MpesaBrokerRequest {
    @XmlAttribute
    private String version;

    @XmlElement(name = "request")
    private Request request;
}
