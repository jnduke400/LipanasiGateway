package com.hybrid9.pg.Lipanasi.serviceImpl.operators;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.repositories.operators.MobileMoneyChannelRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class MobileMoneyChannelServiceImpl {
    private final MobileMoneyChannelRepository mobileMoneyChannelRepository;
    @Transactional(readOnly = true)
    @Cacheable(value = "mobileMoneyChannel", key = "#mobileMoneyChannelId")
    public MobileMoneyChannel findById(Long mobileMoneyChannelId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MobileMoneyChannel mobileMoneyChannel = this.mobileMoneyChannelRepository.findById(mobileMoneyChannelId).orElse(new MobileMoneyChannel());
        CustomRoutingDataSource.clearCurrentDataSource();
        return mobileMoneyChannel;
    }
    @Transactional(readOnly = true)
    public MobileMoneyChannel findByType(PaymentChannel paymentChannel) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<MobileMoneyChannel> mobileMoneyChannel = this.mobileMoneyChannelRepository.findByType(paymentChannel);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mobileMoneyChannel.orElseThrow(() -> new RuntimeException("Mobile Money Channel not found"));
    }


}
