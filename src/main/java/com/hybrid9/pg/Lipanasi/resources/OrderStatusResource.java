package com.hybrid9.pg.Lipanasi.resources;

import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.serviceImpl.order.OrderSessionServiceImpl;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class OrderStatusResource {

    @Autowired
    @Qualifier("ioExecutor")
    private Executor ioExecutor;
    @Value("${order.session.expiry.default:30}")
    private Integer DEFAULT_SESSION_EXPIRY;
   // private final Integer DEFAULT_SESSION_EXPIRY = 30;

    private final OrderSessionServiceImpl orderSessionService;
    private final OrderService orderService;
    private final SessionManagementService sessionManagementService;

    public OrderStatusResource(OrderSessionServiceImpl orderSessionService,OrderService orderService,SessionManagementService sessionManagementService) {
        this.orderSessionService = orderSessionService;
        this.orderService = orderService;
        this.sessionManagementService = sessionManagementService;
    }

    public void validateOrderStatus(String orderNumber) {
        if (orderNumber == null) {
            throw new OrderResource.InvalidRequestException("Order request is required");
        }
    }

    public void validateOrderStatus(String orderNumber, Optional<String> authorizationHeader) {
        if (orderNumber == null) {
            throw new OrderResource.InvalidRequestException("Order request is required");
        }
        this.checkIfHasValidCredentials(authorizationHeader);
        // This is a dummy response
        // In a real application, this would be a call to a service that would validate the order
        /**
         * This is a dummy response
         * In a real application, this would be a call to a service that would validate the order
         */
        // return "{\"partnerId\":\"1234\",\"apiKey\":\"1234\",\"status\":\"active\"}";
        return;

    }

    private void checkIfHasValidCredentials(Optional<String> authorizationHeader) {
        authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
        if (!(authorizationHeader.get().startsWith("Basic "))) {
            throw new OrderResource.InvalidRequestException("Credentials are required");
        }
    }

    public CompletableFuture<Boolean> checkIfSessionIsValid(Optional<String> authorizationHeader, String orderNumber) {
        return CompletableFuture.supplyAsync(() -> {
            authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
            //Optional<OrderSession> orderSession = this.orderSessionService.findByCredentialsAndOrderEmail(authorizationHeader.get().substring(6), orderRequestDto.getEmail());
           // return orderSession.map(session -> session.getStatus().equals(OrderSessionStatus.ACTIVE)).orElse(false);
            Order orderResult = this.orderService.findByOrderNumber(orderNumber).orElseThrow(() -> new CustomExcpts.OrderNotFoundException("Order not found"));
            return this.sessionManagementService.getSession(orderResult.getPaymentSessionId()).map(session -> session.getLastAccessedAt().isAfter(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))).orElse(false);
        }, ioExecutor);
    }


    public CompletableFuture<Boolean> checkIfSessionIsValid(String orderNumber) {
        return CompletableFuture.supplyAsync(() -> {
            Order orderResult = this.orderService.findByOrderNumber(orderNumber).orElseThrow(() -> new CustomExcpts.OrderNotFoundException("Order not found"));
            return this.sessionManagementService.getSession(orderResult.getPaymentSessionId()).map(session -> session.getLastAccessedAt().isAfter(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))).orElse(false);
        }, ioExecutor);
    }



    public String composeInvalidResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failed");
        response.put("message", message);
        return response.toString();
    }


    public Object composeResponse(AtomicReference<Order> orderReference, Optional<String> authorizationHeader, Boolean isValid) {
        authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
        Map<String, Object> response = new HashMap<>();
        response.put("orderNumber", orderReference.get().getOrderNumber());
        response.put("orderToken", orderReference.get().getOrderToken());
        response.put("amount", orderReference.get().getAmount());
        response.put("currency", orderReference.get().getCurrency());
        response.put("metadata", orderReference.get().getMetadata());
        response.put("paymentMethod", orderReference.get().getPaymentMethod().getType().name());
        response.put("receipt", orderReference.get().getReceipt());
        response.put("status", isValid ? "active" : "inactive");
        response.put("message", isValid ? "Order is Valid" : "Order is Invalid");
        response.put("sessionId", orderReference.get().getPaymentSessionId());
        response.put("sessionStatus", isValid ? "active" : "inactive");

        // Construct Customer Object
        Map<String, Object> customer = new HashMap<>();
        customer.put("firstName", orderReference.get().getCustomer().getFirstName());
        customer.put("lastName", orderReference.get().getCustomer().getLastName());
        customer.put("email", orderReference.get().getCustomer().getEmail());
        customer.put("phoneNumber", orderReference.get().getCustomer().getPhoneNumber());

        // Add Customer Object to Response
        response.put("customer", customer);
        return response;
    }

    public Object composeResponse(AtomicReference<Order> orderReference, Boolean isValid) {
        //authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
        Map<String, Object> response = new HashMap<>();
        response.put("orderNumber", orderReference.get().getOrderNumber());
        response.put("orderToken", orderReference.get().getOrderToken());
        response.put("amount", orderReference.get().getAmount());
        response.put("currency", orderReference.get().getCurrency());
        response.put("metadata", orderReference.get().getMetadata());
        response.put("paymentMethod", orderReference.get().getPaymentMethod().getType().name());
        response.put("receipt", orderReference.get().getReceipt());
        response.put("status", isValid ? "active" : "inactive");
        response.put("message", isValid ? "Order is Valid" : "Order is Invalid");
        response.put("sessionId", orderReference.get().getPaymentSessionId());
        response.put("sessionStatus", isValid ? "active" : "inactive");

        // Construct Customer Object
        Map<String, Object> customer = new HashMap<>();
        customer.put("firstName", orderReference.get().getCustomer().getFirstName());
        customer.put("lastName", orderReference.get().getCustomer().getLastName());
        customer.put("email", orderReference.get().getCustomer().getEmail());
        customer.put("phoneNumber", orderReference.get().getCustomer().getPhoneNumber());

        // Add Customer Object to Response
        response.put("customer", customer);
        return response;
    }

    public CompletableFuture<Object> getOrder(String orderNumber, Boolean isValid) {
        AtomicReference<Order> orderReference = new AtomicReference<>();
        return CompletableFuture.supplyAsync(() -> {
            Order order = this.orderService.findByOrderNumber(orderNumber).orElseThrow(() -> new RuntimeException("Order not found"));
            if (order != null) {
                orderReference.set(order);
                return this.composeResponse(orderReference,isValid);
            } else {
                return null;
            }
        }, ioExecutor);

    }
}
