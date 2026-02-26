package com.hybrid9.pg.Lipanasi.repositories.order;

import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findBySignature(String substring);

    Optional<Order> findByOrderNumber(String orderNumber);

    Order findBySignatureAndCustomer(String substring, Customer customer);

    Order findBySignatureAndOrderToken(String substring, String orderToken);

    Optional<Order> findByPaymentSessionId(String sessionId);

    Optional<Order> findBySignatureAndReceipt(String substring, String receipt);

    Optional<Order> findTop1BySignature(String substring);

    Order findByPayBillReference(String customerReferenceId);

    Optional<Order> findByReceipt(String customerReferenceId);
}