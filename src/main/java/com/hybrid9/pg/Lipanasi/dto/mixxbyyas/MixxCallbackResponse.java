package com.hybrid9.pg.Lipanasi.dto.mixxbyyas;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MixxCallbackResponse {
    private String responseCode;
    private Boolean responseStatus;
    private String responseDescription;
    private String referenceID;
}
