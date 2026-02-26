package com.hybrid9.pg.Lipanasi.events;

import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.MpesaBroker;
import org.springframework.context.ApplicationEvent;

public class MpesaCallbackEvent extends ApplicationEvent {
    public MpesaBroker request;
    public MpesaCallbackEvent(Object source,MpesaBroker request) {
        super(source);
        this.request = request;
    }

    public MpesaBroker getRequest() {
        return request;
    }

    public void setRequest(MpesaBroker request) {
        this.request = request;
    }
}
