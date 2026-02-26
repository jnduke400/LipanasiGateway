package com.hybrid9.pg.Lipanasi.route.processor.mpesapaybill;

import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.MpesaBroker;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Response;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component("mpesa-response-processor")
public class ResponseProcessor {

    private final PayBillPaymentService payBillPaymentService;

    public ResponseProcessor(PayBillPaymentService payBillPaymentService) {
        this.payBillPaymentService = payBillPaymentService;
    }

    public void process(Exchange exchange) throws Exception {
        MpesaBroker response = exchange.getMessage().getBody(MpesaBroker.class);

        if (response != null && response.getResponse() != null) {
            Response mpesaResponse = response.getResponse();

            // Validate response fields
            if (!"0".equals(mpesaResponse.getResponseCode())) {
                throw new RuntimeException("Invalid response code from M-Pesa: " +
                        mpesaResponse.getResponseCode());
            }

            /*if (!"Attempted successfully".equals(mpesaResponse.getResponseDesc())) {
                throw new RuntimeException("Unexpected response description: " +
                        mpesaResponse.getResponseDesc());
            }*/

            // Add additional validation as needed
            this.payBillPaymentService.findPayBillByValidationId(response.getResponse().getOriginatorConversationID()).ifPresent(payBillPayment -> {
                payBillPayment.setCollectionStatus(CollectionStatus.COLLECTED);
               this.payBillPaymentService.update(payBillPayment);
            });

        } else {
            throw new RuntimeException("Invalid or empty response from M-Pesa");
        }
    }

}
