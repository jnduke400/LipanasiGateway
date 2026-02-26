package com.hybrid9.pg.Lipanasi.services.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;

import java.util.Optional;

public interface VendorService {
    Optional<VendorDetails> findVendorDetailsByCode(String vendorCode);

    VendorDetails registerVendorDetails(VendorDetails vendorDetails);

    VendorDetails findVendorDetailsByVendorExternalId(String partnerId);

    VendorDetails updateVendorDetails(VendorDetails vendorDetails);
}
