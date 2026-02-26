package com.hybrid9.pg.Lipanasi.repositories.bank;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardPaymentRepository extends JpaRepository<CardPayment, Long> {
    List<CardPayment> findByCollectionStatusInAndBankNameIn(List<CollectionStatus> collectionStatusList, List<String> bankNameList);

    Optional<CardPayment> findByPaymentReferenceAndTransactionIdAndBankNameAndAmount(String reference, String transactionId, String bankName, double amount);

    Optional<CardPayment> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference);

    CardPayment findByPaymentReference(String reference);

    Optional<CardPayment> findBySessionId(String sessionId);
}
