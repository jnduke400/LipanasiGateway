package com.hybrid9.pg.Lipanasi.dto.mpesapaybill;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

@Data
public class Request {
    @XmlElement(name = "serviceProvider")
    private ServiceProvider serviceProvider;

    @XmlElement(name = "transaction")
    private Transaction transaction;
}
