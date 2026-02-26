package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.repositories.payments.CashInLogRepository;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class CashInLogServiceImpl implements CashInLogService {
    private final CashInLogRepository cashInLogRepository;

    @Transactional
    @Override
    public CashInLog recordLog(CashInLog cashInLog) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        cashInLog = this.cashInLogRepository.save(cashInLog);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cashInLog;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<CashInLog> findById(Long cashInLogId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CashInLog> cashInLog = this.cashInLogRepository.findById(cashInLogId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cashInLog;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<CashInLog> findByPaymentReference(String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CashInLog> cashInLog = this.cashInLogRepository.findByPaymentReference(reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cashInLog;
    }
}
