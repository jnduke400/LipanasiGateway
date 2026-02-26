package com.hybrid9.pg.Lipanasi.services.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;

public interface MainAccountService {
    MainAccount findMainAccountByVendorDetailsAndAccountNumber(VendorDetails vendorDetails, String accountNumber);

    MainAccount findByVendorDetails(VendorDetails vendorDetailsData);

    MainAccount update(MainAccount balanceData);

    MainAccount findMainAccountByAccountNumber(String number);

    void createMainAccount(MainAccount mainAccount);

    MainAccount findTopAccounts();
}
