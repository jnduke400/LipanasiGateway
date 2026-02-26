package com.hybrid9.pg.Lipanasi.route.processor.bank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.bank.BillingInformationDTO;
import com.hybrid9.pg.Lipanasi.dto.bank.CardPaymentDto;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.services.bank.CardPaymentService;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
@Slf4j
public class BankProcessor {
    public static void process(Exchange exchange, CardPaymentService cardPaymentService, Gson gson) {

        //System.out.println("pushUssd " + exchange.getIn().getBody(PushUssd.class).getMsisdn());
        CardPaymentDto cardPaymentDto = CardPaymentDto.builder()
                .id(exchange.getIn().getBody(CardPayment.class).getId())
                .paymentReference(exchange.getIn().getBody(CardPayment.class).getPaymentReference())
                .originalReference(exchange.getIn().getBody(CardPayment.class).getOriginalReference())
                .transactionId(exchange.getIn().getBody(CardPayment.class).getTransactionId())
                .currency(exchange.getIn().getBody(CardPayment.class).getCurrency())
                .amount(exchange.getIn().getBody(CardPayment.class).getAmount())
                .bankName(exchange.getIn().getBody(CardPayment.class).getBankName())
                .bankId(exchange.getIn().getBody(CardPayment.class).getBankId())
                .collectionType(exchange.getIn().getBody(CardPayment.class).getCollectionType())
                //.cardToken(exchange.getIn().getBody(CardPayment.class).getCardToken())
                .channel(exchange.getIn().getBody(CardPayment.class).getChannel())
                .sessionId(exchange.getIn().getBody(CardPayment.class).getSessionId())
                .errorMessage(exchange.getIn().getBody(CardPayment.class).getErrorMessage())
                .collectionStatus(exchange.getIn().getBody(CardPayment.class).getCollectionStatus())
                .vendorDto(VendorDto.builder()
                        /*.vendorName(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getVendorName())
                        .vendorCode(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getVendorCode())
                        .billNumber(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getBillNumber())*/
                        .hasCommission(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getHasCommission())
                        .hasVat(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getHasVat())
                        .charges(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getCharges())
                        .externalId(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getVendorExternalId())
                        .callbackUrl(exchange.getIn().getBody(CardPayment.class).getVendorDetails().getCallbackUrl())
                        .build())
                .billingInformation(BillingInformationDTO.builder()
                        .firstName(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getFirstName())
                        .lastName(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getLastName())
                        .address1(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getAddress1())
                        .city(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getCity())
                        .state(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getState())
                        .postalCode(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getPostalCode())
                        .country(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getCountry())
                        .email(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getEmail())
                        .phone(exchange.getIn().getBody(CardPayment.class).getBillingInformation().getPhone())
                        .build())
                .build();


        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(cardPaymentDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        exchange.getIn().setBody(json);

    }
}
