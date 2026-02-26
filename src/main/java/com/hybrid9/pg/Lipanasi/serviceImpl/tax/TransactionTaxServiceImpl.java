package com.hybrid9.pg.Lipanasi.serviceImpl.tax;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.tax.TransactionTax;
import com.hybrid9.pg.Lipanasi.repositories.tax.TransactionTaxRepository;
import com.hybrid9.pg.Lipanasi.services.tax.TransactionTaxService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class TransactionTaxServiceImpl implements TransactionTaxService {
    private TransactionTaxRepository transactionTaxRepository;

    @Transactional(readOnly = true)
    @Override
    public Optional<TransactionTax> findByPaymentReference(String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<TransactionTax> transactionTax = this.transactionTaxRepository.findByPaymentReference(reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transactionTax;
    }

    @Transactional
    @Override
    public TransactionTax createTransactionTax(TransactionTax transactionTax) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        TransactionTax transactionTax1 = this.transactionTaxRepository.save(transactionTax);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transactionTax1;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TransactionTax> findById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<TransactionTax> transactionTax = this.transactionTaxRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transactionTax;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TransactionTax> findByTaxRate(Long taxRate) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<TransactionTax> transactionTax = this.transactionTaxRepository.findByTaxRate(taxRate);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transactionTax;
    }
    @Transactional
    @Override
    public void recordTax(TransactionTax transactionTax) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.transactionTaxRepository.save(transactionTax);
        CustomRoutingDataSource.clearCurrentDataSource();
    }

}
