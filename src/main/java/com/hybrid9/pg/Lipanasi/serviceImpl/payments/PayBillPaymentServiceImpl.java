package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.repositories.payments.PayBillPaymentRepository;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PayBillPaymentServiceImpl implements PayBillPaymentService {
    private final PayBillPaymentRepository payBillPaymentRepository;

    @Transactional(readOnly = true)
    @Override
    public Optional<PayBillPayment> findById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PayBillPayment> payBillPayment = payBillPaymentRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment;
    }

    @Transactional(readOnly = true)
    //@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "databaseOperations", fallbackMethod = "findByCollectionStatusAndOperatorFallback")
    @Override
    public List<PayBillPayment> findByCollectionStatusAndOperator(List<CollectionStatus> collectionStatusList, List<String> mnoList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PayBillPayment> payBillPayments = payBillPaymentRepository.findTop1500ByCollectionStatusInAndOperatorIn(collectionStatusList, mnoList);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayments;
    }
    @Transactional
    @Override
    public List<PayBillPayment> updateAllCollectionStatus(List<PayBillPayment> payBillPayments) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.payBillPaymentRepository.saveAll(payBillPayments);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayments;
    }
    @Transactional
    @Override
    public PayBillPayment update(PayBillPayment payBillPayment1) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.payBillPaymentRepository.save(payBillPayment1);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment1;
    }
    @Transactional
    @Override
    public PayBillPayment createPayBill(PayBillPayment payBillPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.payBillPaymentRepository.save(payBillPayment);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment;
    }
    @Transactional(readOnly = true)
    @Override
    public List<PayBillPayment> findByCollectionStatusAndOperatorTest(List<CollectionStatus> collectionStatusList, List<String> mnoList, String number) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PayBillPayment> payBillPayments = payBillPaymentRepository.findTop1500ByCollectionStatusInAndOperatorInAndMsisdn(collectionStatusList, mnoList, number);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayments;
    }
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public List<PayBillPayment> findByCollectionStatusAndOperatorWithLock(List<CollectionStatus> collectionStatusList, List<String> mnoList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<PayBillPayment> payBillPayments = payBillPaymentRepository.findTop1500ByCollectionStatusInAndOperatorInWithLock(collectionStatusList, mnoList);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayments;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PayBillPayment> findPayBillByValidationId(String reference1) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PayBillPayment> payBillPayment = payBillPaymentRepository.findByValidationId(reference1);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PayBillPayment> findPayBillByValidationIdAndMsisdn(String txnId, String msisdn) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PayBillPayment> payBillPayment = payBillPaymentRepository.findByValidationIdAndMsisdn(txnId, msisdn);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PayBillPayment> findPayBillByReceiptNumber(String mpesaReceipt) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PayBillPayment> payBillPayment = payBillPaymentRepository.findByReceiptNumber(mpesaReceipt);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PayBillPayment> findByPaymentSessionId(String sessionId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PayBillPayment> payBillPayment = payBillPaymentRepository.findBySessionId(sessionId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return payBillPayment;
    }

    /*public List<PayBillPayment> findByCollectionStatusAndOperatorFallback(List<CollectionStatus> collectionStatusList, List<String> mnoList, Exception ex) {
        // Throw custom exception
        throw new CustomExcpts.DatabaseOperationsException("Database operation failed: findByCollectionStatusAndOperator :"+ex.getMessage());
    }*/
}
