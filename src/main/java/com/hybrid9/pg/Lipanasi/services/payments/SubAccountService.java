package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.vendorx.SubAccount;

public interface SubAccountService {
    SubAccount findByVendorDetails(VendorDetails vendorDetailsData);

    SubAccount update(SubAccount balanceData);
}
