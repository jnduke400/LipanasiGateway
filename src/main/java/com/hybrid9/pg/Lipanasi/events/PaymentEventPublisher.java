package com.hybrid9.pg.Lipanasi.events;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentEventPublisher {
    private ApplicationEventPublisher publisher;

    public void publishPushUssdCashEvent(PushUssdCashEvent event){publisher.publishEvent(event);}

    public void publishPushUssdEvent(PushUssdEvent event){publisher.publishEvent(event);}

    public void publishPushUssdSuccessEvent(PushUssdSuccessEvent event){publisher.publishEvent(event);}

    public void publishMpesaCallbackEvent(MpesaCallbackEvent event){publisher.publishEvent(event);}
}
