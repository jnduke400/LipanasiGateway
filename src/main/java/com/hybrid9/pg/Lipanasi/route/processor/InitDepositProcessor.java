package com.hybrid9.pg.Lipanasi.route.processor;

import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.nimbusds.jose.shaded.gson.Gson;
import org.apache.camel.Exchange;

public class InitDepositProcessor {

    public static void process(Exchange exchange,Gson gson) {

        try {
            Deposit deposit = exchange.getIn().getBody(Deposit.class);
            DepositDto depositDto = DepositDto.builder()
                    .id(deposit.getId())
                    .msisdn(deposit.getMsisdn())
                    .amount(deposit.getAmount())
                    .channel(deposit.getChannel())
                    .paymentReference(deposit.getPaymentReference())
                    .transactionId(deposit.getTransactionId())
                    .operator(deposit.getOperator())
                    .requestStatus(deposit.getRequestStatus())
                    .vendorDto(VendorDto.builder()
                            .vendorName(deposit.getVendorDetails().getVendorName())
                            .vendorCode(deposit.getVendorDetails().getVendorCode())
                            .billNumber(deposit.getVendorDetails().getBillNumber())
                            .hasCommission(deposit.getVendorDetails().getHasCommission())
                            .hasVat(deposit.getVendorDetails().getHasVat())
                            .charges(deposit.getVendorDetails().getCharges())
                            .build())
                    .build();

            exchange.getIn().setBody(gson.toJson(depositDto));
        } catch (Exception e) {
            // Proper error handling
            exchange.setException(new RuntimeException("Failed to process deposit: " + e.getMessage(), e));
        }
    }
}
