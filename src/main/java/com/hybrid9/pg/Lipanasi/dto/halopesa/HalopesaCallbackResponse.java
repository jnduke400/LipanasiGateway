package com.hybrid9.pg.Lipanasi.dto.halopesa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HalopesaCallbackResponse {
    private String responseCode;
    private String message;
    private String referenceId;
    private String responseTime;
    private String responseType;
    private String additionData;
}
