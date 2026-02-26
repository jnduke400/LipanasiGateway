package com.hybrid9.pg.Lipanasi.repositories.commission;

import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.print.attribute.standard.MediaName;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CommissionTierRepository extends JpaRepository<CommissionTier, Long> {

    @Query("SELECT ct FROM CommissionTier ct " +
            "WHERE ct.vendor.id = :vendorId " +
            "AND ct.paymentMethod.id = :paymentMethodId " +
            "AND ct.minimumAmount <= :amount " +
            "AND ct.maximumAmount >= :amount " +
            "AND ct.paymentChannel.id = :paymentChannelId " +
            "AND ct.operator.id = :operatorId " +
            "AND ct.isActive = true")
    Optional<CommissionTier> findApplicableTier(
            @Param("vendorId") Long vendorId,
            @Param("paymentMethodId") Long paymentMethodId,
            @Param("amount") BigDecimal amount,
            @Param("paymentChannelId") Long paymentChannelId,
            @Param("operatorId") Long operatorId);

    CommissionTier findByVendor(VendorDetails vendorDetails);

    CommissionTier findByVendorAndOperatorAndPaymentChannel(VendorDetails vendorDetails, MnoMapping operator, MobileMoneyChannel paymentChannelResult);
}
