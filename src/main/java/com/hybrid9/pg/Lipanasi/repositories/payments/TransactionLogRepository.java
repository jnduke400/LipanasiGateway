package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByMsisdnAndChannelAndPaymentReference(String msisdn, PaymentChannel channelId, String reference);

    Optional<Transaction> findByVendorDetails(VendorDetails vendorDetails);


    Optional<Transaction> findByMsisdnAndChannelAndPaymentReferenceAndVendorDetails(String msisdn, PaymentChannel paymentChannel, String paymentReference, VendorDetails vendorDetails);

    Optional<Transaction> findByChannelAndPaymentReferenceAndVendorDetails(PaymentChannel paymentChannel, String paymentReference, VendorDetails vendorDetails);

    Optional<Transaction> findByChannelAndPaymentReference(PaymentChannel paymentChannel, String paymentReference);
}