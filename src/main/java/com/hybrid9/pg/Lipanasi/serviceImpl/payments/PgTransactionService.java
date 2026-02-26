package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.gw.GatewayTransaction;
import com.hybrid9.pg.Lipanasi.repositories.payments.GatewayTransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PgTransactionService {
    private final GatewayTransactionRepository transactionRepository;

    @Transactional
    public void createTransaction(GatewayTransaction transaction) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        transactionRepository.save(transaction);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    public Optional<GatewayTransaction> findById(String transactionId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<GatewayTransaction> transaction = transactionRepository.findById(transactionId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transaction;
    }
}
