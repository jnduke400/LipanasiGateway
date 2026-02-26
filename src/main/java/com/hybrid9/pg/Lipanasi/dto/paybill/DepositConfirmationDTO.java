package com.hybrid9.pg.Lipanasi.dto.paybill;

import lombok.Data;

@Data
public class DepositConfirmationDTO {
    private int status;
    private Object data;
    private String message;
}
