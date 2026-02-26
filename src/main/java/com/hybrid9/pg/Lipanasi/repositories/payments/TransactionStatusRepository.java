package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.dto.TransactionStatusDTO;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionStatusRepository extends JpaRepository<Deposit, Long> {
    @Query("SELECT new com.hybrid9.pg.Lipanasi.dto.TransactionStatusDTO(" +
            "d.originalReference, d.operator, d.currency, d.channel, " +
            "p.event, p.collectionStatus, d.amount, p.message, t.thirdPartyResponse, d.msisdn) " +
            "FROM Deposit d " +
            "LEFT JOIN PushUssd p ON d.paymentReference = p.reference " +
            "LEFT JOIN Transaction t ON d.paymentReference = t.paymentReference " +
            "WHERE d.originalReference = :originalReference")
    Optional<TransactionStatusDTO> findTransactionStatus(@Param("originalReference") String originalReference);
}
