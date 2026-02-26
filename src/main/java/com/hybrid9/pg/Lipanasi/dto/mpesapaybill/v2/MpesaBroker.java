package com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@XmlRootElement(name = "mpesaBroker")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class MpesaBroker {
    @XmlAttribute
    private String xmlns = "http://inforwise.co.tz/broker/";

    @XmlAttribute
    private String version = "2.0";

    @XmlElement(name = "request")
    private Request request;

    @XmlElement(name = "response")
    private Response response;

    @XmlElement(name = "result")
    private Result result;
}
