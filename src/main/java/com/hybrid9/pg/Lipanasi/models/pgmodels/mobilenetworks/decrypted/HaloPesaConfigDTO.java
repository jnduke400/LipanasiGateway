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
public class HaloPesaConfigDTO {
    @JsonProperty("sp_id")
    private String spId;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("business_number")
    private String businessNumber;
    @JsonProperty("beneficiary_account_id")
    private String beneficiaryAccountId;
    @JsonProperty("secret_key")
    private String secretKey;
    @JsonProperty("merchant_code")
    private String merchantCode;
    @JsonProperty("api_url")
    private String apiUrl;
    @JsonProperty("tqs_url")
    private String tqsUrl;
    @JsonProperty("status")
    private String status;
}
