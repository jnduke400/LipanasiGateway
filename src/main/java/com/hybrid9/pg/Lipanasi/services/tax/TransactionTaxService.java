package com.hybrid9.pg.Lipanasi.services.tax;

import com.hybrid9.pg.Lipanasi.entities.payments.tax.TransactionTax;

import java.util.Optional;

public interface TransactionTaxService {

    Optional<TransactionTax> findByPaymentReference(String reference);

    TransactionTax createTransactionTax(TransactionTax transactionTax);

    Optional<TransactionTax> findById(Long id);

    Optional<TransactionTax> findByTaxRate(Long taxRate);

    void recordTax(TransactionTax transactionTax);
}
