package com.hybrid9.pg.Lipanasi.dto.deposit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepositResponse {
    private String reference;
    private String status;
    private String message;
}
