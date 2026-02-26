package com.hybrid9.pg.Lipanasi.services.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.dto.PayBillPaymentDto;
import com.hybrid9.pg.Lipanasi.dto.PushUssdDto;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.repositories.payments.PayBillPaymentRepository;
import com.hybrid9.pg.Lipanasi.services.DeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PayBillDeduplicationService {
    private final PayBillPaymentRepository payBillPaymentRepository;

    public PayBillDeduplicationService(PayBillPaymentRepository payBillPaymentRepository) {
        this.payBillPaymentRepository = payBillPaymentRepository;
    }


    private static final Logger log = LoggerFactory.getLogger(PayBillDeduplicationService.class);

    @Transactional
    public String checkAndMarkDeposited(String payBillDto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        PayBillPaymentDto message = mapper.readValue(payBillDto, PayBillPaymentDto.class);
        // Check if message for this customer/campaign already sent
        boolean alreadyDeposited = this.checkMessageHistory(
                message.getPaymentReference(),
                message.getMsisdn(),
                message.getOperator(),
                message.getAmount()
        );

        if (alreadyDeposited) {
            // Log duplicate message if any

            log.info("Transaction already deposited for {}, from {}, with mobile number {}", message.getPaymentReference(), message.getOperator(), message.getMsisdn());
            return null;  // Filtering out duplicate
        }

        return mapper.writeValueAsString(message);
    }

    @Transactional
    private boolean checkMessageHistory(String reference, String msisdn, String operator, double amount) {
        AtomicBoolean alreadyDeposited = new AtomicBoolean(false);
        this.payBillPaymentRepository.findByPaymentReferenceAndMsisdnAndOperatorAndAmount(reference, msisdn, operator, amount).ifPresent(payBillPayment -> {
            if (payBillPayment.getCollectionStatus() != CollectionStatus.NEW && payBillPayment.getCollectionStatus() != CollectionStatus.PROCESSING && payBillPayment.getCollectionStatus() != CollectionStatus.COLLECTED && payBillPayment.getCollectionStatus() != CollectionStatus.FAILED) {
                alreadyDeposited.set(true);
                payBillPayment.setCollectionStatus(CollectionStatus.DUPLICATE);
                this.payBillPaymentRepository.save(payBillPayment);
                System.out.println("alreadyDeposited = " + alreadyDeposited);
            } else {
                alreadyDeposited.set(false);
                System.out.println("alreadyDeposited = " + alreadyDeposited);
            }
        });
        return alreadyDeposited.get();
    }
}
