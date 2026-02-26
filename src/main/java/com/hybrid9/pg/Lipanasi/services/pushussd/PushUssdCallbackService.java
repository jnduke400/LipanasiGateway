package com.hybrid9.pg.Lipanasi.services.pushussd;


import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdCallback;
import com.hybrid9.pg.Lipanasi.repositories.pushussd.PushUssdCallbackRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
public class PushUssdCallbackService {
    private PushUssdCallbackRepository callbackRepository;
    @Transactional
    public void newCallback(PushUssdCallback ussdCallback) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.callbackRepository.save(ussdCallback);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
}
