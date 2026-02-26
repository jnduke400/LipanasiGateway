package com.hybrid9.pg.Lipanasi.component.halopesac2bresponse;

import lombok.Builder;

@Builder
public class PaymentResponse {
    private Header header;
    private Body body;

    // Getters and Setters
    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}
