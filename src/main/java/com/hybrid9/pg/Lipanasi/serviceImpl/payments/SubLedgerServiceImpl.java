package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.payments.SubLedger;
import com.hybrid9.pg.Lipanasi.repositories.payments.SubLedgerRepository;
import com.hybrid9.pg.Lipanasi.services.payments.SubLedgerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SubLedgerServiceImpl implements SubLedgerService {
    private final SubLedgerRepository subLedgerRepository;
    @Transactional(readOnly = true)
    @Override
    public SubLedger findByLastAdded(VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        SubLedger subLedger = this.subLedgerRepository.findByVendorDetailsIdAndLastAdded(vendorDetails.getId());
        CustomRoutingDataSource.clearCurrentDataSource();
        return subLedger;

    }
    @Transactional
    @Override
    public void addLedger(SubLedger ledger) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.subLedgerRepository.save(ledger);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
}
