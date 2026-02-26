package com.hybrid9.pg.Lipanasi.repositories.tax;

import com.hybrid9.pg.Lipanasi.entities.payments.tax.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
}