package com.hybrid9.pg.Lipanasi.serviceImpl.vendorx;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import com.hybrid9.pg.Lipanasi.repositories.vendorx.CustomerRepository;
import com.hybrid9.pg.Lipanasi.services.vendorx.CustomerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    @Transactional
    @Override
    public Customer createCustomer(Customer customer) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        Customer customer1 = this.customerRepository.save(customer);
        CustomRoutingDataSource.clearCurrentDataSource();
        return customer1;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Customer> findByEmail(String email) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Customer> customer = this.customerRepository.findByEmail(email);
        CustomRoutingDataSource.clearCurrentDataSource();
        return customer;
    }

    @Override
    public Optional<Customer> findByEmailAndPhoneNumber(String email, String phoneNumber) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Customer> customer = this.customerRepository.findByEmailAndPhoneNumber(email, phoneNumber);
        CustomRoutingDataSource.clearCurrentDataSource();
        return customer;
    }
}
