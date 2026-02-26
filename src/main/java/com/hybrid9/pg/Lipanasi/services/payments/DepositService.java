package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

public interface DepositService {
    Deposit recordDeposit(Deposit deposit);

    List<Deposit> findByRequestStatusAndOperator(List<RequestStatus> requestStatuses, List<String> mnoList);

    List<Deposit> updateAllRequestStatus(List<Deposit> deposits);

    Optional<Deposit> findByTransactionId(String transactionNumber);

    Optional<Deposit> findByReference(String reference);

    Optional<Deposit> findByMsisdnAndChannelAndPaymentReference(String msisdn, PaymentChannel paymentChannel, String reference);

    Deposit update(Deposit deposit);

    List<Deposit> findByRequestStatusAndOperatorTest(List<RequestStatus> requestStatuses, List<String> mnoList);

    Optional<Deposit> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference);

    Optional<Deposit> findBySessionId(String sessionId);
}
