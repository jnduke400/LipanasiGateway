package com.hybrid9.pg.Lipanasi.services.order;

import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;

import java.util.Optional;

public interface OrderService {
    Order createOrder(Order order);

    Order findBySignature(String substring);

    Optional<Order> findBySignatureWithOptional(String substring);

    Optional<Order> findByOrderNumber(String orderNumber);

    void updateOrder(Order orderResult);


    Order findBySignatureAndCustomer(String substring, Customer customer);

    Order findBySignatureAndOrderToken(String substring, String orderToken);

    Optional<Order> findByPaymentSessionId(String sessionId);

    Order findByPayBillReference(String customerReferenceId);

    Optional<Order> findByReceipt(String customerReferenceId);
}
