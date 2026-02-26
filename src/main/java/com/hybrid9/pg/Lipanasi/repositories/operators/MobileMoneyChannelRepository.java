package com.hybrid9.pg.Lipanasi.repositories.operators;

import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileMoneyChannelRepository extends JpaRepository<MobileMoneyChannel, Long> {
    Optional<MobileMoneyChannel> findByType(PaymentChannel paymentChannel);
}
