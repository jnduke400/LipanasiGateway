package com.hybrid9.pg.Lipanasi.repositories.tax;

import com.hybrid9.pg.Lipanasi.entities.payments.tax.TransactionTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionTaxRepository extends JpaRepository<TransactionTax, Long> {
    Optional<TransactionTax> findByPaymentReference(String reference);

    Optional<TransactionTax> findByTaxRate(Long taxRate);
}