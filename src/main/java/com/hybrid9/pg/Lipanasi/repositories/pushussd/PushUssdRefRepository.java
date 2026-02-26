package com.hybrid9.pg.Lipanasi.repositories.pushussd;

import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushUssdRefRepository extends JpaRepository<PushUssdRef, Long> {
    PushUssdRef findByMapReference(String reference);
}