package com.hybrid9.pg.Lipanasi.repositories.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}