package com.hybrid9.pg.Lipanasi.serviceImpl.vendorx;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Business;
import com.hybrid9.pg.Lipanasi.repositories.vendorx.BusinessRepository;
import com.hybrid9.pg.Lipanasi.services.vendorx.BusinessService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class BusinessServiceImpl implements BusinessService {
    private final BusinessRepository businessRepository;
    @Transactional(readOnly = true)
    @Override
    public Business findBusinessByName(String name) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Business business = this.businessRepository.findByName(name);
        CustomRoutingDataSource.clearCurrentDataSource();
        return business;
    }
    @Transactional
    @Override
    public Business registerBusiness(Business business) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        business = businessRepository.save(business);
        CustomRoutingDataSource.clearCurrentDataSource();
        return business;
    }
    @Transactional
    @Override
    public Business updateBusiness(Business business) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        business = businessRepository.save(business);
        CustomRoutingDataSource.clearCurrentDataSource();
        return business;
    }
}
