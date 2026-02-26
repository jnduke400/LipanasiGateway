package com.hybrid9.pg.Lipanasi.repositories.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MainAccountRepository extends JpaRepository<MainAccount, Long> {
    MainAccount findMainAccountByVendorDetailsAndAccountNumber(VendorDetails vendorDetails, String accountNumber);


    MainAccount findByVendorDetails(VendorDetails vendorDetailsData);

    MainAccount findMainAccountByAccountNumber(String number);

    MainAccount findTop1ByOrderByAccountNumberDesc();
}