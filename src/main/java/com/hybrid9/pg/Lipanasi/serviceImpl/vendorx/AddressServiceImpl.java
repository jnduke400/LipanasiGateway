package com.hybrid9.pg.Lipanasi.serviceImpl.vendorx;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Address;
import com.hybrid9.pg.Lipanasi.repositories.vendorx.AddressRepository;
import com.hybrid9.pg.Lipanasi.services.vendorx.AddressService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;

    @Transactional
    @Override
    public void createAddress(Address address) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.addressRepository.save(address);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Address> findById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Address> address = this.addressRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return address;
    }
    @Transactional
    @Override
    public Address registerAddress(Address address) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        address = this.addressRepository.save(address);
        CustomRoutingDataSource.clearCurrentDataSource();
        return address;
    }

    @Override
    public Address updateAdress(Address address) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        address = this.addressRepository.save(address);
        CustomRoutingDataSource.clearCurrentDataSource();
        return address;
    }
}
