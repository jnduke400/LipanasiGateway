package com.hybrid9.pg.Lipanasi.serviceImpl.commission;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.repositories.commission.CommissionTierRepository;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CommissionTierServiceImpl implements CommissionTierService {
    private final CommissionTierRepository commissionTierRepository;

    @Transactional(readOnly = true)
    @Override
    public Optional<CommissionTier> findApplicableTier(Long id, Long paymentMethodId, BigDecimal amount, Long paymentChannelId, Long operator) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CommissionTier> commissionTier = this.commissionTierRepository.findApplicableTier(id, paymentMethodId, amount,paymentChannelId,operator);
        CustomRoutingDataSource.clearCurrentDataSource();
        return commissionTier;
    }

    @Transactional(readOnly = true)
    @Override
    public CommissionTier findCommissionTierByVendorDetails(VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        CommissionTier commissionTier = this.commissionTierRepository.findByVendor(vendorDetails);
        CustomRoutingDataSource.clearCurrentDataSource();
        return commissionTier;
    }

    @Transactional
    @Override
    public CommissionTier updateCommissionTier(CommissionTier commissionTier) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        CommissionTier commissionTierResult = this.commissionTierRepository.save(commissionTier);
        CustomRoutingDataSource.clearCurrentDataSource();
        return commissionTierResult;
    }

    @Transactional
    @Override
    public CommissionTier createCommissionTier(CommissionTier commissionTier) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        CommissionTier commissionTierResult = this.commissionTierRepository.save(commissionTier);
        CustomRoutingDataSource.clearCurrentDataSource();
        return commissionTierResult;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<CommissionTier> findByCommissionTireId(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CommissionTier> commissionTier = this.commissionTierRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return commissionTier;
    }
    @Transactional(readOnly = true)
    @Override
    public CommissionTier findCommissionTierByVendorDetailsAndOperatorAndPaymentChannel(VendorDetails vendorDetails, MnoMapping operator, MobileMoneyChannel paymentChannelResult) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        CommissionTier commissionTier = this.commissionTierRepository.findByVendorAndOperatorAndPaymentChannel(vendorDetails, operator, paymentChannelResult);
        CustomRoutingDataSource.clearCurrentDataSource();
        return commissionTier;
    }
}
