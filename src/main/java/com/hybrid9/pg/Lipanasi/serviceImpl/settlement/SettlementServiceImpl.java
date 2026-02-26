package com.hybrid9.pg.Lipanasi.serviceImpl.settlement;

import com.hybrid9.pg.Lipanasi.repositories.settlement.SettlementTransactionRepository;
import com.hybrid9.pg.Lipanasi.services.settlement.SettlementService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SettlementServiceImpl implements SettlementService {
    private SettlementTransactionRepository settlementTransactionRepository;
}
