package com.hybrid9.pg.Lipanasi.dto.airtelmoney;

import lombok.Data;
@Data
public class AirtelMoneyCallbackResponse {
    private Transaction transaction;

    @Data
    public static class Transaction {
        private String id;
        private String message;
        private String status_code;
        private String airtel_money_id;
        private String code;
    }
}
