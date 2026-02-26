package com.hybrid9.pg.Lipanasi.services.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.PayBillPaymentDto;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.repositories.payments.DepositRepository;
import com.hybrid9.pg.Lipanasi.repositories.payments.PayBillPaymentRepository;
import com.hybrid9.pg.Lipanasi.services.DeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class InitDepositDeduplicationService {
    Gson gson = new Gson();
    private final DepositRepository depositRepository;

    public InitDepositDeduplicationService(DepositRepository depositRepository) {
        this.depositRepository = depositRepository;
    }

    private static final Logger log = LoggerFactory.getLogger(InitDepositDeduplicationService.class);

    @Transactional
    public String checkAndMarkInitDeposited(String depositDto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        DepositDto message = mapper.readValue(depositDto, DepositDto.class);
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
        this.depositRepository.findByPaymentReferenceAndMsisdnAndOperatorAndAmount(reference, msisdn, operator, amount).ifPresent(deposit -> {
            if (deposit.getRequestStatus() != RequestStatus.NEW && deposit.getRequestStatus() != RequestStatus.INITIATED) {
                alreadyDeposited.set(true);
                deposit.setRequestStatus(RequestStatus.DUPLICATE);
                this.depositRepository.save(deposit);
                System.out.println("alreadyDeposited = " + alreadyDeposited);
            } else {
                alreadyDeposited.set(false);
                System.out.println("alreadyDeposited = " + alreadyDeposited);
            }
        });
        return alreadyDeposited.get();
    }
}
