package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedDeposits;
import com.hybrid9.pg.Lipanasi.repositories.payments.FailedDepositsRepository;
import com.hybrid9.pg.Lipanasi.services.payments.FailedDepositsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class FailedDepositsServiceImpl implements FailedDepositsService {
    private final FailedDepositsRepository failedDepositsRepository;
    @Transactional
    @Override
    public void createFailedDeposit(FailedDeposits body) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        failedDepositsRepository.save(body);
        CustomRoutingDataSource.clearCurrentDataSource();

    }
}
