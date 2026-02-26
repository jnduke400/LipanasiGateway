package com.hybrid9.pg.Lipanasi.route.processor.mixbyyas;

import com.hybrid9.pg.Lipanasi.route.handler.mixbyyas.BillerPaymentRequestBuilder;
import org.apache.camel.Exchange;

import java.math.BigDecimal;
import java.util.Map;

public class PaymentRequestProcessor {
    public void process(Exchange exchange) {
        // Extract data from incoming message
        /*Map<String, Object> requestData = exchange.getMessage().getBody(Map.class);*/

        BillerPaymentRequestBuilder.PaymentRequestInput input = new BillerPaymentRequestBuilder.PaymentRequestInput();
        input.setCustomerMsisdn(exchange.getProperty("msisdn",String.class));
        input.setBillerMsisdn(exchange.getProperty("billerMsisdn",String.class));
        input.setAmount(new BigDecimal(exchange.getProperty("amount",String.class)));
        input.setRemarks(exchange.getProperty("remarks",String.class));
        input.setReferenceId(exchange.getProperty("reference",String.class));

        // Store in exchange property
        exchange.setProperty("paymentInput", input);
    }
}
