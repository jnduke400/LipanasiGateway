package com.hybrid9.pg.Lipanasi.repositories.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<VendorDetails, Long> {
    Optional<VendorDetails> findByVendorCode(String vendorCode);

    Optional<VendorDetails> findByVendorExternalId(String partnerId);
}