package com.hybrid9.pg.Lipanasi.dto.orange.callback;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlRootElement(name = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@XmlAccessorType(XmlAccessType.FIELD)
public class OrangeCallbackRequest {

    @XmlElement(name = "Body", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
    private Body body;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Body {
        @XmlElement(name = "doCallback", namespace = "http://servicecb.ws.com/")
        private DoCallback doCallback;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DoCallback {
        private String subsmsisdn;
        private String PartnId;
        private String mermsisdn;
        private String transid;
        private String systemid;
        private String currency;
        private Double amount;
        private String ResultCode;
        private String ResultDesc;
    }
}
