package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedDeposits;

public interface FailedDepositsService {
    void createFailedDeposit(FailedDeposits body);
}
