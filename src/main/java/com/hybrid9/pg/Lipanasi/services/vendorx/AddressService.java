package com.hybrid9.pg.Lipanasi.services.vendorx;



import com.hybrid9.pg.Lipanasi.entities.vendorx.Address;

import java.util.Optional;

public interface AddressService {
    void createAddress(Address address);
    Optional<Address> findById(Long id);

    Address registerAddress(Address address);

    Address updateAdress(Address address);
}
