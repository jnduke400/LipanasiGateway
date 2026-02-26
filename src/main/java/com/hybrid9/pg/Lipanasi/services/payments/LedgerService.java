package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.payments.Ledger;

public interface LedgerService {
    Ledger findByLastAdded(VendorDetails vendorDetailsData);

    void addLedger(Ledger ledger);
}
