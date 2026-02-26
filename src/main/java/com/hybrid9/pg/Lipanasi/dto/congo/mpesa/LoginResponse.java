package com.hybrid9.pg.Lipanasi.dto.congo.mpesa;

import lombok.Data;

@Data
public class LoginResponse {
    private String sessionId;
    private String code;
    private String description;
    private String detail;
    private String transactionId;
}
