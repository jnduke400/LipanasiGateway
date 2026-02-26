package com.hybrid9.pg.Lipanasi.serviceImpl.operators;


import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.repositories.operators.MnoPrefixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrefixServiceImpl {
    @Autowired
    MnoPrefixRepository prefixRepository;
    @Transactional(readOnly = true)
    public MnoPrefix findUsingPrefix(String prefix) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoPrefix mno_prefix = this.prefixRepository.findByPrefix(prefix);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mno_prefix;
    }
    @Transactional(readOnly = true)
    public MnoPrefix findTop1ByMnoMapping(MnoMapping mnoMapping) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoPrefix mno_prefix = this.prefixRepository.findTop1ByMnoMapping(mnoMapping);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mno_prefix;
    }
    @Transactional(readOnly = true)
    public List<MnoPrefix> findAllByMnoMapping(MnoMapping mnoMapping) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<MnoPrefix> prefixList = this.prefixRepository.findAllByMnoMapping(mnoMapping);
        CustomRoutingDataSource.clearCurrentDataSource();
        return prefixList;
    }
    @Transactional(readOnly = true)
    public List<MnoPrefix> findAllPrefix() {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        List<MnoPrefix> prefixList = this.prefixRepository.findAll();
        CustomRoutingDataSource.clearCurrentDataSource();
        return prefixList;
    }
    @Transactional(readOnly = true)
    public MnoPrefix findById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoPrefix mno_prefix = this.prefixRepository.findById(id).orElseThrow(()->new RuntimeException("Prefix not found"));
        CustomRoutingDataSource.clearCurrentDataSource();
        return mno_prefix;
    }

    @Transactional
    public void savePrefixs(List<MnoPrefix> prefixList) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        prefixRepository.saveAll(prefixList);
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    public MnoPrefix savePrefix(MnoPrefix mnoPrefix) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        MnoPrefix byPrefix = this.prefixRepository.save(mnoPrefix);
        CustomRoutingDataSource.clearCurrentDataSource();
        return byPrefix;
    }

    @Transactional
    public void remove(MnoPrefix mnoPrefix) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.prefixRepository.delete(mnoPrefix);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    public Page<MnoPrefix> findAllPrefix(String searchTerm, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Pageable pageRequest = createPageRequest(page, limit, sort, order);
        Page<MnoPrefix> pageResult = this.prefixRepository.findAllPrefix(searchTerm, pageRequest);
        CustomRoutingDataSource.clearCurrentDataSource();
        return pageResult;
    }

    private Pageable createPageRequest(int page, int limit, String sort, String order) {
        if (order.equalsIgnoreCase("asc")) {
            return PageRequest.of(page, limit, Sort.Direction.ASC, sort);

        } else {
            return PageRequest.of(page, limit, Sort.Direction.DESC, sort);

        }

    }
}
