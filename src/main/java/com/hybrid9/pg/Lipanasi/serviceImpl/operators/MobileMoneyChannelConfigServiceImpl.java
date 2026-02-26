package com.hybrid9.pg.Lipanasi.serviceImpl.operators;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannelConfig;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.repositories.operators.MobileMoneyChannelConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class MobileMoneyChannelConfigServiceImpl {
    private final MobileMoneyChannelConfigRepository mobileMoneyChannelConfigRepository;

    @Transactional(readOnly = true)
    public Optional<MobileMoneyChannelConfig> findByMobileMoneyChannelAndVendor(MobileMoneyChannel channelType, VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<MobileMoneyChannelConfig> mobileMoneyChannelConfig = this.mobileMoneyChannelConfigRepository.findByMobileMoneyChannelAndVendor(channelType, vendorDetails);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mobileMoneyChannelConfig;
    }
    @Transactional(readOnly = true)
    public Optional<MobileMoneyChannelConfig> findByMobileMoneyChannelAndVendorAndMobileOperator(MobileMoneyChannel byType, VendorDetails vendorDetails, MnoMapping byName) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<MobileMoneyChannelConfig> mobileMoneyChannelConfig = this.mobileMoneyChannelConfigRepository.findByMobileMoneyChannelAndVendorAndMobileOperator(byType, vendorDetails, byName);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mobileMoneyChannelConfig;
    }
    @Transactional
    public void createOrUpdateMobileMoneyChannelConfig(List<MobileMoneyChannelConfig> mobileMoneyChannelConfigs) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.mobileMoneyChannelConfigRepository.saveAll(mobileMoneyChannelConfigs);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
}
