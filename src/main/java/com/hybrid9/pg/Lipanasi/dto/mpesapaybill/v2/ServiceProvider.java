package com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ServiceProvider {
    private String spId;
    private String spPassword;
    private String timestamp;
}
