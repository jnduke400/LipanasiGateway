package com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement(name = "COMMAND")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class EnquiryResponse {
    @XmlElement(name = "STATUS")
    private String status;

    @XmlElement(name = "MESSAGE")
    private String message;

    @XmlElement(name = "REF")
    private String ref;
}
