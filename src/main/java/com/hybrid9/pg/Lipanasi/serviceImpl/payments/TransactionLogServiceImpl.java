package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.Transaction;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.repositories.payments.TransactionLogRepository;
import com.hybrid9.pg.Lipanasi.services.bank.CardPaymentService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.services.payments.TransactionLogService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class TransactionLogServiceImpl implements TransactionLogService {
    private final TransactionLogRepository transactionLogRepository;
    private final PushUssdService pushUssdService;
    private final CardPaymentService cardPaymentService;
    private final PayBillPaymentService payBillPaymentService;

    @Transactional
    @Override
    public Transaction recordTransaction(Transaction transaction) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        transaction = transactionLogRepository.save(transaction);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transaction;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Transaction> findByMsisdnAndChannelAndPaymentReference(String msisdn, PaymentChannel channel, String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Transaction> transaction = transactionLogRepository.findByMsisdnAndChannelAndPaymentReference(msisdn, channel, reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transaction;

    }

    @Transactional
    @Override
    public void updateTransaction(Transaction transaction) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        transactionLogRepository.save(transaction);
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    @Override
    public void updateRetryCount(PushUssd pushUssd) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        pushUssdService.findPushUssdById(pushUssd.getId()).ifPresent(pushUssd1 -> {
            this.transactionLogRepository.findByMsisdnAndChannelAndPaymentReferenceAndVendorDetails(pushUssd1.getMsisdn(), PaymentChannel.PUSH_USSD, pushUssd1.getReference(), pushUssd1.getVendorDetails()).ifPresent(transactionLog -> {
                transactionLog.setRetryCount(transactionLog.getRetryCount() + 1);
                transactionLog.setRequestStatus(RequestStatus.MARKED_FOR_RETRY);
                transactionLogRepository.save(transactionLog);
            });

        });
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    @Override
    public void updateToFailed(PushUssd pushUssd) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        pushUssdService.findPushUssdById(pushUssd.getId()).ifPresent(pushUssd1 -> {
            this.transactionLogRepository.findByMsisdnAndChannelAndPaymentReferenceAndVendorDetails(pushUssd1.getMsisdn(), PaymentChannel.PUSH_USSD, pushUssd1.getReference(), pushUssd1.getVendorDetails()).ifPresent(transactionLog -> {
                /* transaction.setRetryCount(transaction.getRetryCount() + 1);*/
                transactionLog.setRequestStatus(RequestStatus.FAILED);
                transactionLogRepository.save(transactionLog);
            });

        });
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    @Override
    public void updateRetryCount(PayBillPayment billPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        payBillPaymentService.findById(billPayment.getId()).ifPresent(payBillPayment1 -> {
            this.transactionLogRepository.findByMsisdnAndChannelAndPaymentReferenceAndVendorDetails(payBillPayment1.getMsisdn(), PaymentChannel.PAY_BILL, payBillPayment1.getPaymentReference(), payBillPayment1.getVendorDetails()).ifPresent(transactionLog -> {
                transactionLog.setRetryCount(transactionLog.getRetryCount() + 1);
                transactionLog.setRequestStatus(RequestStatus.MARKED_FOR_RETRY);
                transactionLogRepository.save(transactionLog);
            });

        });
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    @Override
    public void updateToFailed(PayBillPayment billPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        payBillPaymentService.findById(billPayment.getId()).ifPresent(payBillPayment1 -> {
            this.transactionLogRepository.findByMsisdnAndChannelAndPaymentReferenceAndVendorDetails(payBillPayment1.getMsisdn(), PaymentChannel.PAY_BILL, payBillPayment1.getPaymentReference(), payBillPayment1.getVendorDetails()).ifPresent(transactionLog -> {
                /* transaction.setRetryCount(transaction.getRetryCount() + 1);*/
                transactionLog.setRequestStatus(RequestStatus.FAILED);
                transactionLogRepository.save(transactionLog);
            });

        });
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    @Override
    public void updateRetryCount(CardPayment cardPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        cardPaymentService.findCardPaymentById(cardPayment.getId()).ifPresent(cardPayment1 -> {
            this.transactionLogRepository.findByChannelAndPaymentReferenceAndVendorDetails(PaymentChannel.BANK_PAYMENT_GATEWAY, cardPayment1.getPaymentReference(), cardPayment1.getVendorDetails()).ifPresent(transactionLog -> {
                transactionLog.setRetryCount(transactionLog.getRetryCount() + 1);
                transactionLog.setRequestStatus(RequestStatus.MARKED_FOR_RETRY);
                transactionLogRepository.save(transactionLog);
            });

        });
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional
    @Override
    public void updateToFailed(CardPayment cardPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        cardPaymentService.findCardPaymentById(cardPayment.getId()).ifPresent(cardPayment1 -> {
            this.transactionLogRepository.findByChannelAndPaymentReferenceAndVendorDetails(PaymentChannel.PUSH_USSD, cardPayment1.getPaymentReference(), cardPayment1.getVendorDetails()).ifPresent(transactionLog -> {
                /* transaction.setRetryCount(transaction.getRetryCount() + 1);*/
                transactionLog.setRequestStatus(RequestStatus.FAILED);
                transactionLogRepository.save(transactionLog);
            });

        });
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Transaction> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<Transaction> transaction = transactionLogRepository.findByChannelAndPaymentReference(paymentChannel, paymentReference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return transaction;
    }


}
