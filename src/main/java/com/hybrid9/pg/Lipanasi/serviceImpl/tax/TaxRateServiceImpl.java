package com.hybrid9.pg.Lipanasi.serviceImpl.tax;

import com.hybrid9.pg.Lipanasi.repositories.tax.TaxRateRepository;
import com.hybrid9.pg.Lipanasi.services.tax.TaxRateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TaxRateServiceImpl implements TaxRateService {
    private TaxRateRepository taxRateRepository;
}
