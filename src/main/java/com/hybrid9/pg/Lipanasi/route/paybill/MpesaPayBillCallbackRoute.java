package com.hybrid9.pg.Lipanasi.route.paybill;

import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MpesaPayBillCallbackRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:mpesa-paybill-callback")
                .log("Mpesa PayBill Callback received: ${body}")
                .to(CamelConfiguration.RABBIT_PRODUCER_MPESA_PAY_BILL_CALLBACK_URI);
    }
}
