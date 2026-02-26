package com.hybrid9.pg.Lipanasi.repositories.settlement;

import com.hybrid9.pg.Lipanasi.entities.payments.settlemets.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}