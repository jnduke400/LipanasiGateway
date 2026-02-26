package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.payments.Ledger;
import com.hybrid9.pg.Lipanasi.repositories.payments.LedgerRepository;
import com.hybrid9.pg.Lipanasi.services.payments.LedgerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class LedgerServiceImpl implements LedgerService {
    private final LedgerRepository ledgerRepository;
    @Transactional(readOnly = true)
    @Override
    public Ledger findByLastAdded(VendorDetails vendorDetailsData) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Ledger ledger = this.ledgerRepository.findByVendorDetailsIdAndLastAdded(vendorDetailsData.getId());
        CustomRoutingDataSource.clearCurrentDataSource();
        return ledger;

    }
    @Transactional
    @Override
    public void addLedger(Ledger ledger) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.ledgerRepository.save(ledger);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
}
