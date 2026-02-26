package com.hybrid9.pg.Lipanasi.repositories.pushussd;



import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.interfaces.projections.PushUssdProj;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushUssdRepository extends JpaRepository<PushUssd, Long> {
    PushUssd findByMsisdnAndReferenceAndNonce(String msisdn, String reference, String nonce);

    PushUssd findByCollectionStatus(CollectionStatus collectionStatus);
    @Query(value = "SELECT pu.collection_status as collectionStatus, pu.creation_date as collectionTime, hh.reference as accountId, hh.name as clientName,pu.collection_type as collectionType, pu.amount, pu.msisdn as phoneNumber,pu.invoice_no as invoiceNumber FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where pu.vendor_id = ?1 and (pu.amount like %?2% or pu.msisdn like %?2%  or pu.invoice_no like %?2%  or hh.name like %?2%  or pu.collection_status like %?2% or pu.account_id like %?2% )",
            countQuery = "SELECT count(*) FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where pu.vendor_id = ?1 and (pu.amount like %?2% or pu.msisdn like %?2%  or pu.invoice_no like %?2%  or hh.name like %?2%  or pu.collection_status like %?2% or pu.account_id like %?2% )", nativeQuery = true)
    Page<PushUssdProj> findAllByVendorDetailsAndAllCols(long vendorId, String searchKey, Pageable pageable);

    @Query(value = "SELECT pu.collection_status as collectionStatus, pu.creation_date as collectionTime, hh.reference as accountId, hh.name as clientName,pu.collection_type as collectionType, pu.amount, pu.msisdn as phoneNumber,pu.invoice_no as invoiceNumber FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where pu.vendor_id = ?1 and (pu.amount like %?3% or pu.msisdn like %?2%  or pu.invoice_no like %?7%  or hh.name like %?5%  or pu.collection_status like %?6% or pu.account_id like %?4% )",
            countQuery = "SELECT count(*) FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where pu.vendor_id = ?1 and (pu.amount like %?3% or pu.msisdn like %?2%  or pu.invoice_no like %?7%  or hh.name like %?5%  or pu.collection_status like %?6% or pu.account_id like %?4%)", nativeQuery = true)
    Page<PushUssdProj> findAllByVendorDetailsAndCollectionTypeAndAmountAndAllColumns(long id, String phoneNumber, String amount, String reference, String clientName, String status, String invoiceNo, Pageable pageable);
    @Query(value = "SELECT pu.collection_status as collectionStatus, pu.creation_date as collectionTime, hh.reference as accountId, hh.name as clientName,pu.collection_type as collectionType, pu.amount, pu.msisdn as phoneNumber FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where pu.vendor_id = ?1",
            countQuery = "SELECT count(*) FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where pu.vendor_id = ?1",nativeQuery = true)
    Page<PushUssdProj> findAllByVendorDetails(long vendorId, Pageable pageable);
    @Query(value = "SELECT pu.collection_status as collectionStatus, pu.creation_date as collectionTime, hh.reference as accountId, hh.name as clientName,pu.collection_type as collectionType, pu.amount, pu.msisdn as phoneNumber,pu.invoice_no as invoiceNumber FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where (pu.amount like %?2% or pu.msisdn like %?1%  or pu.invoice_no like %?6%  or hh.name like %?4%  or pu.collection_status like %?5% or pu.account_id like %?3% )",
            countQuery = "SELECT count(*) FROM push_ussd as pu left join house_holders as hh on pu.account_id = hh.reference where (pu.amount like %?2% or pu.msisdn like %?1%  or pu.invoice_no like %?6%  or hh.name like %?4%  or pu.collection_status like %?5% or pu.account_id like %?3%)", nativeQuery = true)
    Page<PushUssdProj> findAllByCollectionStatusAndAmountAndAllColumns(String phoneNumber, String amount, String reference, String clientName, String status, String invoice_no, PageRequest of);

    List<PushUssd> findTop10ByCollectionStatus(CollectionStatus collectionStatus);

    List<PushUssd> findTop10ByCollectionStatusAndCollectionType(CollectionStatus collectionStatus, String collectionType);

    PushUssd findByMsisdnAndReference(String msisdn, String reference);

    List<PushUssd> findTop1500ByCollectionStatusInAndOperatorIn(List<CollectionStatus> collectionStatusList, List<String> mnoList);

    Optional<PushUssd> findByReferenceAndMsisdnAndOperatorAndAmount(String reference, String msisdn, String operator, double amount);

    PushUssd findByReference(String asString);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM PushUssd u WHERE u.collectionStatus IN :statuses AND u.operator IN :operators")
    List<PushUssd> findTop1500ByCollectionStatusInAndOperatorInWithLock(@Param("statuses") List<CollectionStatus> collectionStatusList, @Param("operators")List<String> mnoList);

    @Query(value = "SELECT * FROM c2b_push_ussd WHERE collection_status = 'NEW' AND operator = :operator AND query_attempts < 3 AND creation_date > '2025-03-11' AND creation_date <= NOW() - INTERVAL 5 MINUTE ORDER BY creation_date DESC limit 100", nativeQuery = true)
    List<PushUssd> getNewTransactions(@Param("operator") String operator);

    @Query(value = "SELECT * FROM c2b_push_ussd WHERE collection_status = 'NEW' AND operator = :operator AND query_attempts < 3 AND creation_date > '2025-03-27' AND creation_date <= NOW() - INTERVAL 5 MINUTE ORDER BY creation_date DESC limit 1", nativeQuery = true)
    List<PushUssd> getNewTransactionsTest(@Param("operator") String operator);

    @Query(value = "SELECT * \n" +
            "FROM c2b_push_ussd \n" +
            "WHERE collection_status = 'NEW' \n" +
            " AND message = 'Ussd Push Initiated Successfully' \n" +
            "  AND (operator = 'Mixx_by_yas-Tanzania' OR operator = 'ZPesa-Tanzania') \n" +
            "  AND query_attempts < 3 \n" +
            "  AND creation_date > '2025-02-24' \n" +
            "  AND creation_date <= NOW() - INTERVAL 5 MINUTE\n" +
            "ORDER BY creation_date DESC \n" +
            "LIMIT 100", nativeQuery = true)
    List<PushUssd> getNewTransactions();
    @Query(value = "SELECT * FROM c2b_push_ussd WHERE collection_status IN :collectionStatusList AND operator IN :mnoList LIMIT :batchSize", nativeQuery = true)
    List<PushUssd> findPushUssdByCollectionStatusInAndOperatorIn(List<String> collectionStatusList, List<String> mnoList, int batchSize);

    PushUssd findBySessionId(String sessionId);
}