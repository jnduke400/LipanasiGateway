package com.hybrid9.pg.Lipanasi.serviceImpl.order;


import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.order.OrderSession;
import com.hybrid9.pg.Lipanasi.repositories.order.OrderSessionRepository;
import com.hybrid9.pg.Lipanasi.services.order.OrderSessionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class OrderSessionServiceImpl implements OrderSessionService {
    private final OrderSessionRepository orderSessionRepository;
    @Transactional(readOnly = true)
    @Override
    public Optional<OrderSession> findByCredentials(String substring) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<OrderSession> orderSession = this.orderSessionRepository.findByCredential(substring);
        CustomRoutingDataSource.clearCurrentDataSource();
        return orderSession;
    }
    @Transactional
    @Override
    public void createSession(OrderSession orderSession) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.orderSessionRepository.save(orderSession);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
   @Transactional(readOnly = true)
    @Override
    public Optional<OrderSession> findByCredentialsAndOrderEmail(String substring, String email) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<OrderSession> orderSession = this.orderSessionRepository.findByCredentialAndOrderEmail(substring,email);
        CustomRoutingDataSource.clearCurrentDataSource();
        return orderSession;
    }
}
