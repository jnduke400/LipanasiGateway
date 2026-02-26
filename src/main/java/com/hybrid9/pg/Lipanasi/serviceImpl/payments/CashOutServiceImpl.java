package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.repositories.payments.CashOutRepository;
import com.hybrid9.pg.Lipanasi.services.payments.CashOutService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CashOutServiceImpl implements CashOutService {
    private final CashOutRepository cashOutRepository;
}
