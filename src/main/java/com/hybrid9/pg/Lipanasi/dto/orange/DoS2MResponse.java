package com.hybrid9.pg.Lipanasi.dto.orange;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class DoS2MResponse {
    @JacksonXmlProperty(localName = "return")
    private ReturnData returnData;
}
