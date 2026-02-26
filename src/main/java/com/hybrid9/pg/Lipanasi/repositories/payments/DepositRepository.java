package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
    List<Deposit> findTop1500ByRequestStatusInAndOperatorIn(List<RequestStatus> requestStatuses, List<String> mnoList);

    Optional<Deposit> findByPaymentReferenceAndMsisdnAndOperatorAndAmount(String reference, String msisdn, String operator, double amount);

    Optional<Deposit> findByTransactionId(String transactionNumber);

    Optional<Deposit> findByPaymentReference(String reference);

    Optional<Deposit> findByMsisdnAndChannelAndPaymentReference(String msisdn, PaymentChannel paymentChannel, String reference);

    List<Deposit> findTop1500ByRequestStatusInAndOperatorInAndMsisdn(List<RequestStatus> requestStatuses, List<String> mnoList,String msisdn);

    Optional<Deposit> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference);

    Optional<Deposit> findBySessionId(String sessionId);
}