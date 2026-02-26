package com.hybrid9.pg.Lipanasi.component.halopesac2bresponse;

import lombok.Builder;

@Builder
public class Body {
    private Response response;

    // Getter and Setter
    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
