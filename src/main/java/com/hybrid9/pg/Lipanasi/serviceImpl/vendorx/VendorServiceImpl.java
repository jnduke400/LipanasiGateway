package com.hybrid9.pg.Lipanasi.serviceImpl.vendorx;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.repositories.vendorx.VendorRepository;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class VendorServiceImpl implements VendorService {
    private final VendorRepository vendorRepository;
    @Transactional(readOnly = true)
    @Override
    public Optional<VendorDetails> findVendorDetailsByCode(String vendorCode) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<VendorDetails> vendor = this.vendorRepository.findByVendorCode(vendorCode);
        CustomRoutingDataSource.clearCurrentDataSource();
        return vendor;
    }
    @Transactional
    @Override
    public VendorDetails registerVendorDetails(VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        vendorDetails = this.vendorRepository.save(vendorDetails);
        CustomRoutingDataSource.clearCurrentDataSource();
        return vendorDetails;
    }
    @Transactional(readOnly = true)
    //@Cacheable(value = "vendorDetails", key = "#partnerId")
    @Override
    public VendorDetails findVendorDetailsByVendorExternalId(String partnerId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        VendorDetails vendor = this.vendorRepository.findByVendorExternalId(partnerId).orElse(null);
        CustomRoutingDataSource.clearCurrentDataSource();
        return vendor;
    }

    @Override
    public VendorDetails updateVendorDetails(VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        vendorDetails = this.vendorRepository.save(vendorDetails);
        CustomRoutingDataSource.clearCurrentDataSource();
        return vendorDetails;
    }
}
