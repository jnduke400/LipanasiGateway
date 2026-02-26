package com.hybrid9.pg.Lipanasi.serviceImpl.vendorx;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.repositories.vendorx.MainAccountRepository;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MainAccountServiceImpl implements MainAccountService {
    private final MainAccountRepository mainAccountRepository;
    @Transactional(readOnly = true)
    @Override
    public MainAccount findMainAccountByVendorDetailsAndAccountNumber(VendorDetails vendorDetails, String accountNumber) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MainAccount mainAccount = this.mainAccountRepository.findMainAccountByVendorDetailsAndAccountNumber(vendorDetails, accountNumber);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mainAccount;

    }
    @Transactional(readOnly = true)
    @Override
    public MainAccount findByVendorDetails(VendorDetails vendorDetailsData) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MainAccount mainAccount = this.mainAccountRepository.findByVendorDetails(vendorDetailsData);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mainAccount;

    }
    @Transactional
    @Override
    public MainAccount update(MainAccount balanceData) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        balanceData = this.mainAccountRepository.save(balanceData);
        CustomRoutingDataSource.clearCurrentDataSource();
        return balanceData;
    }
    @Transactional(readOnly = true)
    @Override
    public MainAccount findMainAccountByAccountNumber(String number) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MainAccount mainAccount = this.mainAccountRepository.findMainAccountByAccountNumber(number);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mainAccount;
    }
    @Transactional
    @Override
    public void createMainAccount(MainAccount mainAccount) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.mainAccountRepository.save(mainAccount);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public MainAccount findTopAccounts() {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MainAccount mainAccount = this.mainAccountRepository.findTop1ByOrderByAccountNumberDesc();
        CustomRoutingDataSource.clearCurrentDataSource();
        return mainAccount;
    }
}
