package com.hybrid9.pg.Lipanasi.dto.mixxbyyas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillerPaymentResponse {

    @JsonProperty("ResponseCode")
    private String responseCode;

    @JsonProperty("ResponseStatus")
    private boolean responseStatus;

    @JsonProperty("ResponseDescription")
    private String responseDescription;

    @JsonProperty("ReferenceID")
    private String referenceID;
}
