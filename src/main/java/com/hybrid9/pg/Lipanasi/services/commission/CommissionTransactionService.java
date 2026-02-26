package com.hybrid9.pg.Lipanasi.services.commission;

import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTransaction;

public interface CommissionTransactionService {
    CommissionTransaction recordCommission(CommissionTransaction transaction);
}
