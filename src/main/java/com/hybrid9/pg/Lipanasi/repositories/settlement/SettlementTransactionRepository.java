package com.hybrid9.pg.Lipanasi.repositories.settlement;

import com.hybrid9.pg.Lipanasi.entities.payments.settlemets.SettlementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementTransactionRepository extends JpaRepository<SettlementTransaction, Long> {
}