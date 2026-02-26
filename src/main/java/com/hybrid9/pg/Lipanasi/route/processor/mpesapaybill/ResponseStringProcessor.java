package com.hybrid9.pg.Lipanasi.route.processor.mpesapaybill;

import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.MpesaBroker;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Response;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Component
public class ResponseStringProcessor {
    private final PayBillPaymentService payBillPaymentService;

    public ResponseStringProcessor(PayBillPaymentService payBillPaymentService) {
        this.payBillPaymentService = payBillPaymentService;
    }

    public void process(Exchange exchange) throws Exception {
        String xmlResponse = exchange.getMessage().getBody(String.class);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MpesaBroker.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Remove namespace from XML string to handle it as default namespace
            xmlResponse = xmlResponse.replace("xmlns=\"http://inforwise.co.tz/broker/\"", "");

            StringReader reader = new StringReader(xmlResponse);
            MpesaBroker response = (MpesaBroker) unmarshaller.unmarshal(reader);

            if (response != null && response.getResponse() != null) {
                Response mpesaResponse = response.getResponse();

                // Validate response fields
                if (!"0".equals(mpesaResponse.getResponseCode())) {
                    throw new RuntimeException("Invalid response code from M-Pesa: " +
                            mpesaResponse.getResponseCode());
                }

                if (!"Attempted successfully".equals(mpesaResponse.getResponseDesc())) {
                    throw new RuntimeException("Unexpected response description: " +
                            mpesaResponse.getResponseDesc());
                }

                // Process the payment
                this.payBillPaymentService.findPayBillByValidationId(
                        mpesaResponse.getOriginatorConversationID()
                ).ifPresent(payBillPayment -> {
                    payBillPayment.setCollectionStatus(CollectionStatus.COLLECTED);
                    this.payBillPaymentService.update(payBillPayment);
                });
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to process M-Pesa response", e);
        }
    }
}
