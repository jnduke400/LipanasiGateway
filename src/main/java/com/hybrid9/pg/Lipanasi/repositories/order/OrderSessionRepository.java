package com.hybrid9.pg.Lipanasi.repositories.order;

import com.hybrid9.pg.Lipanasi.entities.order.OrderSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderSessionRepository extends JpaRepository<OrderSession, Long> {
    Optional<OrderSession> findByCredential(String substring);

    Optional<OrderSession> findByCredentialAndOrderEmail(String substring, String email);
}