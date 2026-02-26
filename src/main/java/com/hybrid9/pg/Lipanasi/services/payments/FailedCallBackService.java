package com.hybrid9.pg.Lipanasi.services.payments;

import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedCallBack;

public interface FailedCallBackService {
    void createFailedCallBack(FailedCallBack failedCallBack);
}
