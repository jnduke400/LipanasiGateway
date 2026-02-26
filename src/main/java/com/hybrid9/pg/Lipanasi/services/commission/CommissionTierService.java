package com.hybrid9.pg.Lipanasi.services.commission;

import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;

import java.math.BigDecimal;
import java.util.Optional;

public interface CommissionTierService {
    Optional<CommissionTier> findApplicableTier(Long id, Long paymentMethodId, BigDecimal amount, Long paymentChannelId, Long operator);

    CommissionTier findCommissionTierByVendorDetails(VendorDetails vendorDetails);

    CommissionTier updateCommissionTier(CommissionTier commissionTier);

    CommissionTier createCommissionTier(CommissionTier commissionTier);

    Optional<CommissionTier> findByCommissionTireId(Long id);

    CommissionTier findCommissionTierByVendorDetailsAndOperatorAndPaymentChannel(VendorDetails vendorNotFound, MnoMapping operator, MobileMoneyChannel paymentChannelResult);
}
