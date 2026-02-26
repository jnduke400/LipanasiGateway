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
public class MPesaConfigDTO {

    @JsonProperty("api_url")
    private String apiUrl;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("country")
    private String country;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("token_event_id")
    private String tokenEventId;

    @JsonProperty("request_event_id")
    private String requestEventId;

    @JsonProperty("business_name")
    private String businessName;

    @JsonProperty("business_number")
    private String businessNumber;

    @JsonProperty("paybill_api_base_url")
    private String paybillApiBaseUrl;

    @JsonProperty("paybill_callback_url")
    private String paybillCallbackUrl;

    @JsonProperty("paybill_sp_id")
    private String paybillSpId;

    @JsonProperty("paybill_sp_password")
    private String paybillSpPassword;

    @JsonProperty("token_api_url")
    private String tokenApiUrl;

    @JsonProperty("status")
    private String status;
}

