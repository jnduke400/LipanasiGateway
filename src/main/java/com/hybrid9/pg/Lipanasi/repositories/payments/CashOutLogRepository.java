package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashOutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashOutLogRepository extends JpaRepository<CashOutLog, Long> {
}