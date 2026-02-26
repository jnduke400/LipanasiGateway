package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
  @Query(value = "SELECT l.* FROM c2b_ledgers l WHERE l.vendor_id = ?1 ORDER BY l.id DESC LIMIT 1", nativeQuery = true)
  Ledger findByVendorDetailsIdAndLastAdded(Long id);
}