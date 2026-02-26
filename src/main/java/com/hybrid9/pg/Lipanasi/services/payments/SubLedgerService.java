package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.payments.SubLedger;

public interface SubLedgerService {
    SubLedger findByLastAdded(VendorDetails vendorDetails);

    void addLedger(SubLedger ledger);
}
