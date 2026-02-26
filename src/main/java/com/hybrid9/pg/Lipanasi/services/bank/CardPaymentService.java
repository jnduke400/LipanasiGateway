package com.hybrid9.pg.Lipanasi.services.bank;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

public interface CardPaymentService {
    CardPayment recordCardPayment(CardPayment cardPayment);

    List<CardPayment> findByCollectionStatusAndBankName(List<CollectionStatus> collectionStatusList, List<String> bankNameList);

    List<CardPayment> updateAllCollectionStatus(List<CardPayment> filteredRecords);

    Optional<CardPayment> findPushUssdById(Long id);

    Optional<CardPayment> findCardPaymentById(Long id);

    CardPayment updateCardPayment(CardPayment cardPayment1);

    CardPayment update(CardPayment existingCardPayment);

    Optional<CardPayment> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference);

    CardPayment findByReference(String reference);

    Optional<CardPayment> findBySessionId(String sessionId);
}
