package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.tqs.TigoTransactionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TigoTransactionResponseRepository extends JpaRepository<TigoTransactionResponse, Long> {
  List<TigoTransactionResponse> findByPushUssdId(Long pushUssdId);

  Optional<TigoTransactionResponse> findFirstByPushUssdIdOrderByResponseDateDesc(Long pushUssdId);

  @Query("SELECT t FROM TigoTransactionResponse t WHERE t.pushUssd.id = :pushUssdId AND t.creationDate >= :startDate")
  List<TigoTransactionResponse> findRecentResponses(@Param("pushUssdId") Long pushUssdId, @Param("startDate") LocalDateTime startDate);

  boolean existsByReferenceId(String referenceId);
}