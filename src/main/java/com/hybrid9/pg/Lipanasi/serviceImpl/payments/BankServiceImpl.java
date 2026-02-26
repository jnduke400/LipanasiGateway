package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.repositories.payments.BankRepository;
import com.hybrid9.pg.Lipanasi.services.payments.BankService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BankServiceImpl implements BankService {
    private final BankRepository bankRepository;
}
