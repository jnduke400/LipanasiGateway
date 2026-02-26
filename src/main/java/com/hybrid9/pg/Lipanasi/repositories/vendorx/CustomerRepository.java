package com.hybrid9.pg.Lipanasi.repositories.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByEmailAndPhoneNumber(String email, String phoneNumber);
}