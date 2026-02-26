package com.hybrid9.pg.Lipanasi.dto.tqs;

import lombok.Data;

@Data
public class EPGQueryStatusResponse {
    private Header header;
    private Body body;

    @Data
    public static class Header {
        private String spId;
        private String spPassword;
        private String timestamp;
        private String merchantCode;
    }

    @Data
    public static class Body {
        private Response response;

        @Data
        public static class Response {
            private String transactionNumber;
            private Long gatewayId;
            private String responseCode;
            private String responseStatus;
            private String reference;
            private String receipt;
            private String result;
        }
    }
}
