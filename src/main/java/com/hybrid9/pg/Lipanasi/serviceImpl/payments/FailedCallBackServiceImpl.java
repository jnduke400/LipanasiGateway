package com.hybrid9.pg.Lipanasi.serviceImpl.payments;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.FailedCallBack;
import com.hybrid9.pg.Lipanasi.repositories.payments.FailedCallBackRepository;
import com.hybrid9.pg.Lipanasi.services.payments.FailedCallBackService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class FailedCallBackServiceImpl implements FailedCallBackService {
    private FailedCallBackRepository failedCallBackRepository;
    @Transactional
    @Override
    public void createFailedCallBack(FailedCallBack failedCallBack) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        failedCallBackRepository.save(failedCallBack);
        CustomRoutingDataSource.clearCurrentDataSource();

    }
}
