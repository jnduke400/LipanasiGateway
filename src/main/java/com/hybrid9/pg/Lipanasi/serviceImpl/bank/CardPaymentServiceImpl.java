package com.hybrid9.pg.Lipanasi.serviceImpl.bank;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.repositories.bank.CardPaymentRepository;
import com.hybrid9.pg.Lipanasi.services.bank.CardPaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CardPaymentServiceImpl implements CardPaymentService {
    private final CardPaymentRepository cardPaymentRepository;

    public CardPaymentServiceImpl(CardPaymentRepository cardPaymentRepository) {
        this.cardPaymentRepository = cardPaymentRepository;
    }

    @Transactional
    @Override
    public CardPayment recordCardPayment(CardPayment cardPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        CardPayment card = cardPaymentRepository.save(cardPayment);
        CustomRoutingDataSource.clearCurrentDataSource();
        return card;
    }
    @Transactional(readOnly = true)
    @Override
    public List<CardPayment> findByCollectionStatusAndBankName(List<CollectionStatus> collectionStatusList, List<String> bankNameList) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<CardPayment> cardPayments = cardPaymentRepository.findByCollectionStatusInAndBankNameIn(collectionStatusList, bankNameList);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayments;
    }
    @Transactional
    @Override
    public List<CardPayment> updateAllCollectionStatus(List<CardPayment> cardPayments) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        if (cardPayments == null || cardPayments.isEmpty()) {
            return new ArrayList<>();
        }
        List<CardPayment> updatedCardPayments = this.cardPaymentRepository.saveAll(cardPayments);
        CustomRoutingDataSource.clearCurrentDataSource();
        return updatedCardPayments;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<CardPayment> findPushUssdById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CardPayment> cardPayment = this.cardPaymentRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<CardPayment> findCardPaymentById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CardPayment> cardPayment = this.cardPaymentRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;
    }
    @Transactional
    @Override
    public CardPayment updateCardPayment(CardPayment cardPayment1) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        CardPayment cardPayment = this.cardPaymentRepository.save(cardPayment1);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;
    }
    @Transactional
    @Override
    public CardPayment update(CardPayment existingCardPayment) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        CardPayment cardPayment = this.cardPaymentRepository.save(existingCardPayment);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<CardPayment> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CardPayment> cardPayment = this.cardPaymentRepository.findByChannelAndPaymentReference(paymentChannel, paymentReference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;
       // return this.cardPaymentRepository.findByChannelAndPaymentReference(paymentChannel, paymentReference);
    }
    @Transactional(readOnly = true)
    @Override
    public CardPayment findByReference(String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        CardPayment cardPayment = this.cardPaymentRepository.findByPaymentReference(reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;
    }
   @Transactional(readOnly = true)
    @Override
    public Optional<CardPayment> findBySessionId(String sessionId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<CardPayment> cardPayment = this.cardPaymentRepository.findBySessionId(sessionId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return cardPayment;

    }

}
