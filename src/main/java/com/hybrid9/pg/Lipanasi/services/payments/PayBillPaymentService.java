package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;

import java.util.List;
import java.util.Optional;

public interface PayBillPaymentService {
    Optional<PayBillPayment> findById(Long id);

    List<PayBillPayment> findByCollectionStatusAndOperator(List<CollectionStatus> collectionStatusList, List<String> mnoList);

    List<PayBillPayment> updateAllCollectionStatus(List<PayBillPayment> payBillPayments);

    PayBillPayment update(PayBillPayment payBillPayment1);

    PayBillPayment createPayBill(PayBillPayment payBillPayment);

    List<PayBillPayment> findByCollectionStatusAndOperatorTest(List<CollectionStatus> collectionStatusList, List<String> mnoList, String number);

    List<PayBillPayment> findByCollectionStatusAndOperatorWithLock(List<CollectionStatus> collectionStatusList, List<String> mnoList);

    Optional<PayBillPayment> findPayBillByValidationId(String reference1);

    Optional<PayBillPayment> findPayBillByValidationIdAndMsisdn(String txnId, String msisdn);

    Optional<PayBillPayment> findPayBillByReceiptNumber(String mpesaReceipt);

    Optional<PayBillPayment> findByPaymentSessionId(String sessionId);
}
