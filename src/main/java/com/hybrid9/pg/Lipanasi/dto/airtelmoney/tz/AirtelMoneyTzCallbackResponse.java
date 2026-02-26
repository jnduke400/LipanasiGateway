package com.hybrid9.pg.Lipanasi.dto.airtelmoney.tz;


import lombok.Data;

@Data
public class AirtelMoneyTzCallbackResponse {
    private String country;
    private String mno;
    private Payload payload;

    @Data
    public static class Transaction {
        private String id;
        private String message;
        private String status_code;
        private String airtel_money_id;
        private String code;
    }
    @Data
    public static class Payload {
        private Transaction transaction;
    }


}
