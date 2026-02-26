package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;

public interface PaymentMethodService {
    PaymentMethod findByType(PaymentMethodType creditCard);

}
