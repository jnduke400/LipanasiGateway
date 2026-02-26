package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;

import java.util.Optional;

public interface CashInLogService {
    CashInLog recordLog(CashInLog cashInLog);

    Optional<CashInLog> findById(Long cashInLogId);

    Optional<CashInLog> findByPaymentReference(String reference);
}
