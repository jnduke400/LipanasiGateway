package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.dto.TransactionStatusDTO;
import com.hybrid9.pg.Lipanasi.repositories.payments.TransactionStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
@Service
public class TransactionStatusService {
    private final TransactionStatusRepository transactionStatusRepository;

    public TransactionStatusService(TransactionStatusRepository transactionStatusRepository) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.transactionStatusRepository = transactionStatusRepository;
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    public TransactionStatusDTO checkTransactionStatus(String originalReference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        TransactionStatusDTO transactionStatusDTO = transactionStatusRepository.findTransactionStatus(originalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + originalReference));
        CustomRoutingDataSource.clearCurrentDataSource();
        return transactionStatusDTO;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
