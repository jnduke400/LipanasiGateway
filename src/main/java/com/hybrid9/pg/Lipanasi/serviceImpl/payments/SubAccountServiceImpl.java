package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.vendorx.SubAccount;
import com.hybrid9.pg.Lipanasi.repositories.payments.SubAccountRepository;
import com.hybrid9.pg.Lipanasi.services.payments.SubAccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SubAccountServiceImpl implements SubAccountService {
    private final SubAccountRepository subAccountRepository;
    @Transactional(readOnly = true)
    @Override
    public SubAccount findByVendorDetails(VendorDetails vendorDetailsData) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        SubAccount subAccount = subAccountRepository.findByVendorDetails(vendorDetailsData);
        CustomRoutingDataSource.clearCurrentDataSource();
        return subAccount;

    }
    @Transactional
    @Override
    public SubAccount update(SubAccount balanceData) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        subAccountRepository.save(balanceData);
        CustomRoutingDataSource.clearCurrentDataSource();
        return balanceData;
    }
}
