package com.hybrid9.pg.Lipanasi.dto.mixxbyyas;

import lombok.Data;

@Data
public class TokenResponse {
    private String access_token;
    private String token_type;
    private int expires_in;
}
