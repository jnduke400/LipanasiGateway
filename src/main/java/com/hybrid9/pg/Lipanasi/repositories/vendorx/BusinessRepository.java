package com.hybrid9.pg.Lipanasi.repositories.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {
    Business findByName(String name);
}