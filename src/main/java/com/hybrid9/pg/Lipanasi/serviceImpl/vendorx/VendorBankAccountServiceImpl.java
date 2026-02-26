package com.hybrid9.pg.Lipanasi.serviceImpl.vendorx;

import com.hybrid9.pg.Lipanasi.repositories.vendorx.VendorBankAccountRepository;
import com.hybrid9.pg.Lipanasi.repositories.vendorx.VendorRepository;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorBankAccountService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class VendorBankAccountServiceImpl implements VendorBankAccountService {
    private final VendorBankAccountRepository vendorBankAccountRepository;
    private final VendorRepository vendorRepository;
    private final VendorService vendorService;


}
