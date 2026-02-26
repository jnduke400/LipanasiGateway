package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayBillPaymentRepository extends JpaRepository<PayBillPayment, Long> {
    List<PayBillPayment> findTop1500ByCollectionStatusInAndOperatorIn(List<CollectionStatus> collectionStatus, List<String> operator);

    Optional<PayBillPayment> findByPaymentReferenceAndMsisdnAndOperatorAndAmount(String reference, String msisdn, String operator, double amount);

    List<PayBillPayment> findTop1500ByCollectionStatusInAndOperatorInAndMsisdn(List<CollectionStatus> collectionStatusList, List<String> mnoList, String number);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pb FROM PayBillPayment pb WHERE pb.collectionStatus IN :statuses AND pb.operator IN :operators")
    List<PayBillPayment> findTop1500ByCollectionStatusInAndOperatorInWithLock(@Param("statuses") List<CollectionStatus> collectionStatusList, @Param("operators")List<String> mnoList);
    @Query("SELECT pb FROM PayBillPayment pb WHERE pb.validationId = :reference1 order by pb.id desc limit 1")
    Optional<PayBillPayment> findByValidationId(@Param("reference1") String reference1);
    @Query("SELECT pb FROM PayBillPayment pb WHERE pb.validationId = :txnId AND pb.msisdn = :msisdn order by pb.id desc limit 1")
    Optional<PayBillPayment> findByValidationIdAndMsisdn(@Param("txnId") String txnId, @Param("msisdn") String msisdn);

    Optional<PayBillPayment> findByReceiptNumber(String mpesaReceipt);

    Optional<PayBillPayment> findBySessionId(String sessionId);

}