package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.repositories.payments.CashOutLogRepository;
import com.hybrid9.pg.Lipanasi.services.payments.CashOutLogService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CashOutLogServiceImpl implements CashOutLogService {
    private final CashOutLogRepository cashOutLogRepository;
}
