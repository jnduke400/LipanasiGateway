package com.hybrid9.pg.Lipanasi.repositories.commission;

import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommissionTransactionRepository extends JpaRepository<CommissionTransaction, Long> {
}
