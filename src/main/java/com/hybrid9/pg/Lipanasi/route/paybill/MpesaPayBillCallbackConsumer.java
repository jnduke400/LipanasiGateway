package com.hybrid9.pg.Lipanasi.route.paybill;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Value;

import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.route.processor.mpesapaybill.CallbackProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.mpesapaybill.ResponseProcessor;
import com.hybrid9.pg.Lipanasi.route.processor.mpesapaybill.ResponseStringProcessor;

import org.springframework.stereotype.Component;

@Component("mpesaPayBillCallbackRoute")
public class MpesaPayBillCallbackConsumer extends RouteBuilder {
    @Value("${payment-gateway.mpesa.paybill.callback-url}")
    private String callbackUrl;
    private final CallbackProcessor callbackProcessor;
    private final ResponseStringProcessor responseStringProcessor;
    private final ResponseProcessor responseProcessor;
    private final JaxbDataFormat mpesaDataFormat;

    public MpesaPayBillCallbackConsumer(CallbackProcessor callbackProcessor,
                                     ResponseProcessor responseProcessor,ResponseStringProcessor responseStringProcessor,
                                     JaxbDataFormat mpesaDataFormat) {
        this.callbackProcessor = callbackProcessor;
        this.responseProcessor = responseProcessor;
        this.mpesaDataFormat = mpesaDataFormat;
        this.responseStringProcessor = responseStringProcessor;
    }
    @Override
    public void configure() throws Exception {
        // Error handler
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(10000) // 10 second delay between retries
                .backOffMultiplier(2)
                .useExponentialBackOff());

        // Main route for sending callback
        from(CamelConfiguration.RABBIT_CONSUMER_MPESA_PAY_BILL_CALLBACK_URI)
                .routeId("mpesa-callback-route")
                .process(callbackProcessor::process)
                .marshal(mpesaDataFormat)  // Convert to XML
                .setHeader("Content-Type", constant("text/xml"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .log("Sending callback to MPESA Gateway ${body}")
                .to(callbackUrl+"?bridgeEndpoint=true"+
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false" +
                        "&sslContextParameters=#noopSslContext")
                .unmarshal(mpesaDataFormat)  // Convert response back to object
                .log("Received callback Response from MPESA Gateway ${body}")
                .process(responseProcessor::process)
                .end();
    }
}
