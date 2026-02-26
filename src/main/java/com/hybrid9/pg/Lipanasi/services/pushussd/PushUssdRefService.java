package com.hybrid9.pg.Lipanasi.services.pushussd;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdRef;
import com.hybrid9.pg.Lipanasi.repositories.pushussd.PushUssdRefRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PushUssdRefService {
    private PushUssdRefRepository pushUssdRefRepository;
    @Transactional
    public PushUssdRef addRefMap(PushUssdRef pushUssdRef) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        pushUssdRef = this.pushUssdRefRepository.save(pushUssdRef);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssdRef;
    }
    @Transactional(readOnly = true)
    public PushUssdRef getRefByMappingRef(String reference) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        PushUssdRef pushUssdRef = this.pushUssdRefRepository.findByMapReference(reference);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pushUssdRef;

    }
}
