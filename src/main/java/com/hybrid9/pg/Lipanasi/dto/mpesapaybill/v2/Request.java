package com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Request {
    @XmlElement(name = "serviceProvider")
    private ServiceProvider serviceProvider;
    @XmlElement(name = "transaction")
    private Transaction transaction;
}
