package com.hybrid9.pg.Lipanasi.events;

import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import org.springframework.context.ApplicationEvent;

public class PushUssdCashEvent extends ApplicationEvent {
    public PushUssd pushUssd;
    public PushUssdCashEvent(Object source, PushUssd pushUssd) {
        super(source);
        this.pushUssd = pushUssd;
    }

    public PushUssd getPushUssd() {
        return pushUssd;
    }

    public void setPushUssd(PushUssd pushUssd) {
        this.pushUssd = pushUssd;
    }
}
