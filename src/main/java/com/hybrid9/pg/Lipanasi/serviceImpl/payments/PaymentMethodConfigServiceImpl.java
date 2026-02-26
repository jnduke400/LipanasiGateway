package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.repositories.payments.PaymentMethodConfigRepository;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodConfigService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PaymentMethodConfigServiceImpl implements PaymentMethodConfigService {
    private final PaymentMethodConfigRepository paymentMethodConfigRepository;

    @Transactional
    @Override
    public void createPaymentMethod(PaymentMethodConfigEntity paymentMethodEntity) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.paymentMethodConfigRepository.save(paymentMethodEntity);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PaymentMethodConfigEntity> findByVendorDetails(VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PaymentMethodConfigEntity> paymentMethodConfigEntity = this.paymentMethodConfigRepository.findByVendor(vendorDetails);
        CustomRoutingDataSource.clearCurrentDataSource();
        return paymentMethodConfigEntity;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PaymentMethodConfigEntity> findByPaymentMethod(PaymentMethod byType) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PaymentMethodConfigEntity> paymentMethodConfigEntity = this.paymentMethodConfigRepository.findByPaymentMethod(byType);
        CustomRoutingDataSource.clearCurrentDataSource();
        return paymentMethodConfigEntity;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<PaymentMethodConfigEntity> findByPaymentMethodAndVendor(PaymentMethod byType, VendorDetails vendorDetails) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<PaymentMethodConfigEntity> paymentMethodConfigEntity = this.paymentMethodConfigRepository.findByPaymentMethodAndVendor(byType, vendorDetails);
        CustomRoutingDataSource.clearCurrentDataSource();
        return paymentMethodConfigEntity;
    }
    @Transactional
    @Override
    public void updatePaymentMethod(PaymentMethodConfigEntity paymentMethodConfigEntity) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.paymentMethodConfigRepository.save(paymentMethodConfigEntity);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional
    @Override
    public void createOrUpdatePaymentMethodConfig(List<PaymentMethodConfigEntity> paymentMethodConfigEntities) {
      CustomRoutingDataSource.setCurrentDataSource("primary");
      this.paymentMethodConfigRepository.saveAll(paymentMethodConfigEntities);
      CustomRoutingDataSource.clearCurrentDataSource();
    }
}
