package com.hybrid9.pg.Lipanasi.services.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    Customer createCustomer(Customer customers);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByEmailAndPhoneNumber(String email, String phoneNumber);
}
