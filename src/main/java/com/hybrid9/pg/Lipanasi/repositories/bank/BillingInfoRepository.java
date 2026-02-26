package com.hybrid9.pg.Lipanasi.repositories.bank;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.BillingInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingInfoRepository extends JpaRepository<BillingInformation, Long> {
}
