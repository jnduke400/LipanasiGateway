package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.SubLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SubLedgerRepository extends JpaRepository<SubLedger, Long> {
    @Query(value = "SELECT l.* FROM c2b_sub_ledgers l WHERE l.vendor_id = ?1 ORDER BY l.id DESC LIMIT 1", nativeQuery = true)
    SubLedger findByVendorDetailsIdAndLastAdded(Long id);
}