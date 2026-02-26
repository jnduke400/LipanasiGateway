package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashInLogRepository extends JpaRepository<CashInLog, Long> {
    Optional<CashInLog> findByPaymentReference(String reference);
}