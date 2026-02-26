package com.hybrid9.pg.Lipanasi.serviceImpl.commission;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTransaction;
import com.hybrid9.pg.Lipanasi.repositories.commission.CommissionTransactionRepository;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTransactionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CommissionTransactionServiceImpl implements CommissionTransactionService {
    private final CommissionTransactionRepository commissionTransactionRepository;
    @Transactional
    @Override
    public CommissionTransaction recordCommission(CommissionTransaction transaction) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        transaction = this.commissionTransactionRepository.save(transaction);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transaction;
    }
}
