package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.gw.GatewayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface GatewayTransactionRepository extends JpaRepository<GatewayTransaction, String> {
    List<GatewayTransaction> findByUserId(String userId);
    List<GatewayTransaction> findByMerchantId(String merchantId);
}