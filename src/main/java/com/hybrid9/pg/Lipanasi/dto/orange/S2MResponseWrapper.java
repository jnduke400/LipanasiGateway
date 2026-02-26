package com.hybrid9.pg.Lipanasi.dto.orange;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonRootName("Envelope")
public class S2MResponseWrapper {
    @JacksonXmlProperty(localName = "Body")
    private Body body;
}
