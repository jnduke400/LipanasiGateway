package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PaymentMethodConfigService {
    void createPaymentMethod(PaymentMethodConfigEntity paymentMethodEntity);

    Optional<PaymentMethodConfigEntity> findByVendorDetails(VendorDetails vendorDetails);

    Optional<PaymentMethodConfigEntity> findByPaymentMethod(PaymentMethod byType);

    Optional<PaymentMethodConfigEntity> findByPaymentMethodAndVendor(PaymentMethod byType, VendorDetails vendorDetails);

    void updatePaymentMethod(PaymentMethodConfigEntity paymentMethodConfigEntity);

    void createOrUpdatePaymentMethodConfig(List<PaymentMethodConfigEntity> paymentMethodConfigEntities);
}
