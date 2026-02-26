package com.hybrid9.pg.Lipanasi.route.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.PayBillPaymentDto;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import org.apache.camel.Exchange;

public class PayBillProcessor {
    public static void process(Exchange exchange) {
        PayBillPaymentDto payBillPaymentDto = PayBillPaymentDto.builder()
                .id(exchange.getIn().getBody(PayBillPayment.class).getId())
                .version(exchange.getIn().getBody(PayBillPayment.class).getVersion())
                .sessionId(exchange.getIn().getBody(PayBillPayment.class).getSessionId())
                .status(exchange.getIn().getBody(PayBillPayment.class).getStatus())
                /*.vendorx(exchange.getIn().getBody(PayBillPayment.class).getInstitution())*/
                .vendorDto(VendorDto.builder()
                        .vendorName(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getVendorName())
                        .vendorCode(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getVendorCode())
                        .billNumber(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getBillNumber())
                        .hasCommission(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getHasCommission())
                        .hasVat(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getHasVat())
                        .charges(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getCharges())
                        .externalId(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getVendorExternalId())
                        .callbackUrl(exchange.getIn().getBody(PayBillPayment.class).getVendorDetails().getCallbackUrl())
                        .build())
                .payBillId(exchange.getIn().getBody(PayBillPayment.class).getPayBillId())
                .paymentReference(exchange.getIn().getBody(PayBillPayment.class).getPaymentReference())
                .amount(exchange.getIn().getBody(PayBillPayment.class).getAmount())
                .currency(exchange.getIn().getBody(PayBillPayment.class).getCurrency())
                .msisdn(exchange.getIn().getBody(PayBillPayment.class).getMsisdn())
                .operator(exchange.getIn().getBody(PayBillPayment.class).getOperator())
                .transactionDate(exchange.getIn().getBody(PayBillPayment.class).getTransactionDate())
                .collectionType(exchange.getIn().getBody(PayBillPayment.class).getCollectionType())
                .collectionStatus(exchange.getIn().getBody(PayBillPayment.class).getCollectionStatus())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(payBillPaymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        exchange.getIn().setBody(json);
    }
}
