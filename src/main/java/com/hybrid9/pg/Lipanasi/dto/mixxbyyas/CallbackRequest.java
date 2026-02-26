package com.hybrid9.pg.Lipanasi.dto.mixxbyyas;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackRequest {
    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("MFSTransactionID")
    private String mfsTransactionId;

    @JsonProperty("ReferenceID")
    private String referenceId;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Status")
    private boolean status;
}
