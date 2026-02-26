package com.hybrid9.pg.Lipanasi.serviceImpl.order;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import com.hybrid9.pg.Lipanasi.repositories.order.OrderRepository;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    @Transactional
    @Override
    public Order createOrder(Order order) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        Order order1 = this.orderRepository.save(order);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order1;
    }
    @Transactional(readOnly = true)
    @Override
    public Order findBySignature(String substring) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Order order = this.orderRepository.findBySignature(substring);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Order> findBySignatureWithOptional(String substring) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        //Optional<Order> order = this.orderRepository.findBySignatureAndReceipt(substring,receipt);
        Optional<Order> order = this.orderRepository.findTop1BySignature(substring);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Order> order = this.orderRepository.findByOrderNumber(orderNumber);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }
    @Transactional
    @Override
    public void updateOrder(Order orderResult) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.orderRepository.save(orderResult);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public Order findBySignatureAndCustomer(String substring, Customer customer) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Order order = this.orderRepository.findBySignatureAndCustomer(substring,customer);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }
    @Transactional(readOnly = true)
    @Override
    public Order findBySignatureAndOrderToken(String substring, String orderToken) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Order order = this.orderRepository.findBySignatureAndOrderToken(substring,orderToken);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Order> findByPaymentSessionId(String sessionId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Order> order = this.orderRepository.findByPaymentSessionId(sessionId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }
    @Transactional(readOnly = true)
    @Override
    public Order findByPayBillReference(String customerReferenceId) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        Order order = this.orderRepository.findByPayBillReference(customerReferenceId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return order;
    }

    @Override
    public Optional<Order> findByReceipt(String customerReferenceId) {
        return this.orderRepository.findByReceipt(customerReferenceId);
    }
}
