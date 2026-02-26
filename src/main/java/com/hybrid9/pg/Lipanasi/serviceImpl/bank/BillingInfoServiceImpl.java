package com.hybrid9.pg.Lipanasi.serviceImpl.bank;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.BillingInformation;
import com.hybrid9.pg.Lipanasi.repositories.bank.BillingInfoRepository;
import com.hybrid9.pg.Lipanasi.services.bank.BillingInfoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class BillingInfoServiceImpl implements BillingInfoService {

    private final BillingInfoRepository billingInfoRepository;
    @Transactional
    @Override
    public BillingInformation createBillingInfo(BillingInformation billingInformation) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        BillingInformation billingInformation1 = billingInfoRepository.save(billingInformation);
        CustomRoutingDataSource.clearCurrentDataSource();
        return billingInformation1;
    }
}
