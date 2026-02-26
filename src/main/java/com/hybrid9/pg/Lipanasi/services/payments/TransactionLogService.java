package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.Transaction;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;

import java.util.Optional;

public interface TransactionLogService {
    Transaction recordTransaction(Transaction transaction);

    Optional<Transaction> findByMsisdnAndChannelAndPaymentReference(String msisdn, PaymentChannel channel, String reference);

    void updateTransaction(Transaction transaction);

    void updateRetryCount(PushUssd pushUssd);

    void updateToFailed(PushUssd pushUssd);

    void updateRetryCount(PayBillPayment billPayment);

    void updateToFailed(PayBillPayment billPayment);

    void updateRetryCount(CardPayment cardPayment);

    void updateToFailed(CardPayment cardPayment);

    Optional<Transaction> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference);
}
