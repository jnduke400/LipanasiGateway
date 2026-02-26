package com.hybrid9.pg.Lipanasi.repositories.pushussd;

import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdCallback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushUssdCallbackRepository extends JpaRepository<com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdCallback, Long> {
}