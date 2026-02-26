package com.hybrid9.pg.Lipanasi.serviceImpl.settlement;

import com.hybrid9.pg.Lipanasi.repositories.settlement.SettlementTransactionRepository;
import com.hybrid9.pg.Lipanasi.services.settlement.SettlementTransactionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SettlementTransactionServiceImpl implements SettlementTransactionService {
    private SettlementTransactionRepository settlementTransactionRepository;
}
