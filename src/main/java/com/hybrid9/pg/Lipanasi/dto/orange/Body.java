package com.hybrid9.pg.Lipanasi.dto.orange;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class Body {
    @JacksonXmlProperty(localName = "doS2MResponse")
    private DoS2MResponse doS2MResponse;
}
