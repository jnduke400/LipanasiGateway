package com.hybrid9.pg.Lipanasi.dto.airtelmoney;

import lombok.Data;

@Data
public class AirtelMoneyTokenResponse {
    private String access_token;
    private String token_type;
    private int expires_in;


}
