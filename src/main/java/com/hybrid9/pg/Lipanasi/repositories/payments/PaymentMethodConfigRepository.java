package com.hybrid9.pg.Lipanasi.repositories.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfigEntity,Long> {
    Optional<PaymentMethodConfigEntity> findByVendor(VendorDetails vendorDetails);

    Optional<PaymentMethodConfigEntity> findByPaymentMethod(PaymentMethod byType);

    Optional<PaymentMethodConfigEntity> findByPaymentMethodAndVendor(PaymentMethod byType, VendorDetails vendorDetails);
}
