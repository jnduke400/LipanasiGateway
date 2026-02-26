package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.repositories.payments.PaymentMethodRepository;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodService;
import jakarta.persistence.LockModeType;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional(readOnly = true)
    @Override
    public PaymentMethod findByType(PaymentMethodType type) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        PaymentMethod paymentMethod = this.paymentMethodRepository.findByType(type);
        CustomRoutingDataSource.clearCurrentDataSource();
        return paymentMethod;
    }

}
