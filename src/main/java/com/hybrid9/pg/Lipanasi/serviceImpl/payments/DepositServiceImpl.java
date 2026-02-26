package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.repositories.payments.DepositRepository;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class DepositServiceImpl implements DepositService {
    private final DepositRepository depositRepository;
    @Transactional
    @Override
    public Deposit recordDeposit(Deposit deposit) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        deposit = this.depositRepository.save(deposit);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposit;
    }
    @Transactional(readOnly = true)
    @Override
    public List<Deposit> findByRequestStatusAndOperator(List<RequestStatus> requestStatuses, List<String> mnoList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<Deposit> deposits = this.depositRepository.findTop1500ByRequestStatusInAndOperatorIn(requestStatuses, mnoList);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposits;
    }
    @Transactional
    @Override
    public List<Deposit> updateAllRequestStatus(List<Deposit> deposits) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.depositRepository.saveAll(deposits);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposits;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Deposit> findByTransactionId(String transactionNumber) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Deposit> deposit = this.depositRepository.findByTransactionId(transactionNumber);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposit;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Deposit> findByReference(String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Deposit> deposit = this.depositRepository.findByPaymentReference(reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposit;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Deposit> findByMsisdnAndChannelAndPaymentReference(String msisdn, PaymentChannel paymentChannel, String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Deposit> deposit = this.depositRepository.findByMsisdnAndChannelAndPaymentReference(msisdn, paymentChannel, reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposit;
    }
    @Transactional
    @Override
    public Deposit update(Deposit deposit) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        Deposit deposit1 = this.depositRepository.save(deposit);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposit1;
    }
    @Transactional(readOnly = true)
    @Override
    public List<Deposit> findByRequestStatusAndOperatorTest(List<RequestStatus> requestStatuses, List<String> mnoList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<Deposit> deposits = this.depositRepository.findTop1500ByRequestStatusInAndOperatorInAndMsisdn(requestStatuses, mnoList,"255688044555");
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposits;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Deposit> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Deposit> deposits = this.depositRepository.findByChannelAndPaymentReference(paymentChannel, paymentReference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposits;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Deposit> findBySessionId(String sessionId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Deposit> deposits = this.depositRepository.findBySessionId(sessionId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return deposits;
    }
}
