package com.hybrid9.pg.Lipanasi.route.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.PushUssdDto;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
@Slf4j
public class PushUssdProcessor {
    public static void process(Exchange exchange, PushUssdService pushUssdService, Gson gson) {

        System.out.println("pushUssd " + exchange.getIn().getBody(PushUssd.class).getMsisdn());
        PushUssdDto pushUssdDto = PushUssdDto.builder()
                .id(exchange.getIn().getBody(PushUssd.class).getId())
                .msisdn(exchange.getIn().getBody(PushUssd.class).getMsisdn())
                .version(exchange.getIn().getBody(PushUssd.class).getVersion())
                .isSuccess(exchange.getIn().getBody(PushUssd.class).isSuccess())
                .message(exchange.getIn().getBody(PushUssd.class).getMessage())
                .reference(exchange.getIn().getBody(PushUssd.class).getReference())
                .accountId(exchange.getIn().getBody(PushUssd.class).getAccountId())
                .currency(exchange.getIn().getBody(PushUssd.class).getCurrency())
                .amount(exchange.getIn().getBody(PushUssd.class).getAmount())
                .status(exchange.getIn().getBody(PushUssd.class).getStatus())
                .nonce(exchange.getIn().getBody(PushUssd.class).getNonce())
                .details(exchange.getIn().getBody(PushUssd.class).getDetails())
                .operator(exchange.getIn().getBody(PushUssd.class).getOperator())
                .collectionType(exchange.getIn().getBody(PushUssd.class).getCollectionType())
                .errorMessage(exchange.getIn().getBody(PushUssd.class).getErrorMessage())
                .event(exchange.getIn().getBody(PushUssd.class).getEvent())
                .billingPageUrl(exchange.getIn().getBody(PushUssd.class).getBillingPageUrl())
                .cancelBillingUrl(exchange.getIn().getBody(PushUssd.class).getCancelBillingUrl())
                .expiresAt(exchange.getIn().getBody(PushUssd.class).getExpiresAt())
                .collectionStatus(exchange.getIn().getBody(PushUssd.class).getCollectionStatus())
                .sessionId(exchange.getIn().getBody(PushUssd.class).getSessionId())
                .vendorDto(VendorDto.builder()
                        /*.vendorName(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getVendorName())
                        .vendorCode(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getVendorCode())
                        .billNumber(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getBillNumber())*/
                        .hasCommission(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getHasCommission())
                        .hasVat(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getHasVat())
                        .charges(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getCharges())
                        .externalId(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getVendorExternalId())
                        .callbackUrl(exchange.getIn().getBody(PushUssd.class).getVendorDetails().getCallbackUrl())
                        .build())
                .build();


            /*System.out.println("Json  " + gson.toJson(pushUssdDto));
            exchange.getIn().setBody(gson.toJson(pushUssdDto));*/

        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(pushUssdDto);
            log.info("Json >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        exchange.getIn().setBody(json);

    }
}
