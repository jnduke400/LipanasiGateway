package com.hybrid9.pg.Lipanasi.services.payments.mixxtqs;


import com.hybrid9.pg.Lipanasi.component.mixxtqs.TigoResponseParser;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.payments.tqs.TigoTransactionResponse;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.repositories.payments.TigoTransactionResponseRepository;
import com.hybrid9.pg.Lipanasi.repositories.pushussd.PushUssdRepository;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class TigoTransactionService {

    private TigoResponseParser responseParser;
    private TigoTransactionResponseRepository responseRepository;
    private PushUssdRepository pushUssdRepository;
    private PushUssdService pushUssdService;

    public TigoTransactionService(TigoResponseParser responseParser, TigoTransactionResponseRepository responseRepository, PushUssdRepository pushUssdRepository, PushUssdService pushUssdService) {
        this.responseParser = responseParser;
        this.responseRepository = responseRepository;
        this.pushUssdRepository = pushUssdRepository;
        this.pushUssdService = pushUssdService;
    }

    @Transactional
    public void handleApiResponse(String xmlResponse, Long pushUssdId) {
        try {
            // Parse and validate response
            TigoTransactionResponse response = responseParser.processResponse(xmlResponse, pushUssdId);

            // Save response to database
            responseRepository.save(response);

            // Update push USSD status based on response
            updatePushUssdStatus(pushUssdId, response);

        } catch (TigoResponseParser.TigoParserException e) {
            log.error("Failed to process TigoPesa response for pushUssdId={}: {}",
                    pushUssdId, e.getMessage(), e);

            // In case of parser exception, mark the transaction as FAILED
            try {
                Optional<PushUssd> pushUssdById = this.pushUssdService.findPushUssdById(pushUssdId);
                if (pushUssdById.isPresent()) {
                    PushUssd pushUssd = pushUssdById.get();
                    pushUssd.setCollectionStatus(CollectionStatus.FAILED);
                    pushUssd.setStatus("-1");
                    pushUssd.setEvent("failed");
                    pushUssd.setMessage("Failed to process response: " + e.getMessage());
                    this.pushUssdRepository.save(pushUssd);
                    log.info("Marked transaction as FAILED due to parsing error: pushUssdId={}", pushUssdId);
                }
            } catch (Exception ex) {
                log.error("Failed to update transaction status after parsing error: {}", ex.getMessage(), ex);
            }

            throw e;
        }
    }

    private void updatePushUssdStatus(Long pushUssdId, TigoTransactionResponse response) {
        // Get the appropriate collection status based on the response
        CollectionStatus newStatus = determineCollectionStatus(response);

        // Update push USSD status in database
        Optional<PushUssd> pushUssdById = this.pushUssdService.findPushUssdById(pushUssdId);
        if (pushUssdById.isPresent()) {
            PushUssd pushUssd = pushUssdById.get();
            pushUssd.setCollectionStatus(newStatus);

            // Set status "0" and message for COLLECTED or FAILED statuses
            if (newStatus == CollectionStatus.COLLECTED) {
                pushUssd.setStatus("0");
                pushUssd.setEvent("Success");
                pushUssd.setMessage(response.getResultDesc());
            }
            if(newStatus == CollectionStatus.FAILED){
                pushUssd.setStatus("-1");
                pushUssd.setEvent("failed");
                pushUssd.setMessage(response.getResultDesc());
            }

            // Set transaction ID if available
            if (response.getTransactionId() != null && !response.getTransactionId().isEmpty()) {
                pushUssd.setTqsTransactionId(response.getTransactionId());
            }

            // Save the updated push USSD
            PushUssd savedPushUssd = this.pushUssdRepository.save(pushUssd);
            if (savedPushUssd == null) {
                log.warn("Failed to save push USSD status. Record may have been modified: pushUssdId={}",
                        pushUssdId);
                return;
            }

            log.info("Push USSD status updated: pushUssdId={}, newStatus={}, resultCode={}, txnStatus={}",
                    pushUssdId, newStatus, response.getResultCode(), response.getTransactionStatus());
        } else {
            log.warn("Could not find push USSD with ID: {}", pushUssdId);
        }
    }

    /**
     * Determine the collection status based on the TigoPesa response
     * @param response The parsed TigoPesa response
     * @return The appropriate CollectionStatus enum value
     */
    private CollectionStatus determineCollectionStatus(TigoTransactionResponse response) {
        String resultCode = response.getResultCode();
        String txnStatus = response.getTransactionStatus();

        // Check if it's a failure response (starts with GetSOError or non-zero result code)
        if (resultCode != null && (resultCode.startsWith("GetSOError") || !resultCode.equals("0"))) {
            return CollectionStatus.FAILED;
        }
        // Check if it's a success response with Posted status
        else if (resultCode != null && resultCode.equals("0") && txnStatus != null && txnStatus.equals("Posted")) {
            return CollectionStatus.COLLECTED;
        }
        // For any other case, return PENDING
        else {
            return CollectionStatus.PENDING;
        }
    }
}