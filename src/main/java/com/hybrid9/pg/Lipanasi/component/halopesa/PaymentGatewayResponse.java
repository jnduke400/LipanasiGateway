package com.hybrid9.pg.Lipanasi.component.halopesa;

// Main wrapper class
public class PaymentGatewayResponse {
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
