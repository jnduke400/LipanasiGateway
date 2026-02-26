package com.hybrid9.pg.Lipanasi.events;

import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import org.springframework.context.ApplicationEvent;

public class PushUssdEvent extends ApplicationEvent {
    public PushUssd pushUssd;
    public PushUssdEvent(Object source, PushUssd pushUssd) {
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
