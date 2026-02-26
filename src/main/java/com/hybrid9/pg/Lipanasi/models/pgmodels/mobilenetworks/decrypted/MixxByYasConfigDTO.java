package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.decrypted;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixxByYasConfigDTO {

    @JsonProperty("api_url")
    private String apiUrl;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("token_url")
    private String tokenUrl;

    @JsonProperty("biller_msisdn")
    private String billerMsisdn;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("tqs_api_url")
    private String tqsApiUrl;

    @JsonProperty("tqs_password")
    private String tqsPassword;

    @JsonProperty("status")
    private String status;
}

