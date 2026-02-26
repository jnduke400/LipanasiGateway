package com.hybrid9.pg.Lipanasi.services.order;

import com.hybrid9.pg.Lipanasi.entities.order.OrderSession;

import java.util.Optional;

public interface OrderSessionService {
    Optional<OrderSession> findByCredentials(String substring);

    void createSession(OrderSession orderSession);

    Optional<OrderSession> findByCredentialsAndOrderEmail(String substring, String email);
}
