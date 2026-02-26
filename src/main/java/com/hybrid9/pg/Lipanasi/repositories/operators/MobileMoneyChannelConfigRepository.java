package com.hybrid9.pg.Lipanasi.repositories.operators;

import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannelConfig;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileMoneyChannelConfigRepository extends JpaRepository<MobileMoneyChannelConfig, Long> {
    Optional<MobileMoneyChannelConfig> findByMobileMoneyChannelAndVendor(MobileMoneyChannel channelType, VendorDetails vendorDetails);

    Optional<MobileMoneyChannelConfig> findByMobileMoneyChannelAndVendorAndMobileOperator(MobileMoneyChannel byType, VendorDetails vendorDetails, MnoMapping byName);
}
