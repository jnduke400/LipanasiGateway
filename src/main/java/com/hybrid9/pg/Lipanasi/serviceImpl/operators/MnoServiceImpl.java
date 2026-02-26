package com.hybrid9.pg.Lipanasi.serviceImpl.operators;


import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.repositories.operators.MnoMappingRepository;
import com.hybrid9.pg.Lipanasi.resources.MnoResources;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class MnoServiceImpl {

    private MnoMappingRepository mnoRepository;

    private PrefixServiceImpl prefixService;

    private MnoResources mnoResources;

    @Transactional
    public MnoMapping addMno(MnoMapping mnoMapping) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        MnoMapping byMno = mnoRepository.save(mnoMapping);
        CustomRoutingDataSource.clearCurrentDataSource();
        return byMno;
    }

    @Transactional
    public void updateMno(MnoMapping mnoMapping) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.mnoRepository.save(mnoMapping);
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional
    public void remove(MnoMapping mnoMapping) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.mnoRepository.delete(mnoMapping);
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Transactional(readOnly = true)
    public MnoMapping findMappingByMno(String mno) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoMapping byMno = this.mnoRepository.findByMno(mno);
        CustomRoutingDataSource.clearCurrentDataSource();
        return byMno;
    }

    @Transactional(readOnly = true)
    public String searchMno(String msisdn) {
        /*String prefix = msisdn.substring(0, 5);*/
        String prefix = this.mnoResources.getInternationalMnoPrefix(msisdn);
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoPrefix mno_prefix = prefixService.findUsingPrefix(prefix);
        System.out.println("Prefix Object:- " + String.valueOf(mno_prefix) + " Msisdn:- " + prefix);
        if (mno_prefix != null) {
            Long id = mno_prefix.getMnoMapping().getId();
            Optional<MnoMapping> byId = mnoRepository.findById(id);
            MnoMapping mapping = byId.orElse(new MnoMapping());
            CustomRoutingDataSource.clearCurrentDataSource();
            return mapping.getMno();
        } else {
            return "Unknown";
        }
    }

    @Transactional(readOnly = true)
    public MnoPrefix getMno(String msisdn) {
        /*String prefix = msisdn.substring(0, 5);*/
        String prefix = this.mnoResources.getInternationalMnoPrefix(msisdn);
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoPrefix mno_prefix = prefixService.findUsingPrefix(prefix);
        CustomRoutingDataSource.clearCurrentDataSource();
        return mno_prefix;
    }

    @Transactional(readOnly = true)
    public MnoMapping searchMnoObject(String msisdn) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        String prefix = msisdn.substring(0, 5);
        MnoPrefix mno_prefix = prefixService.findUsingPrefix(prefix);
        System.out.println("Prefix Object:- " + String.valueOf(mno_prefix) + " Msisdn:- " + prefix);
        Long id = mno_prefix.getMnoMapping().getId();
        Optional<MnoMapping> byId = mnoRepository.findById(id);
        MnoMapping mapping = byId.orElse(new MnoMapping());
        CustomRoutingDataSource.clearCurrentDataSource();
        return mapping;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "mnoMapping", key = "#id")
    public MnoMapping findById(Long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoMapping byId = mnoRepository.findById(id).orElse(new MnoMapping());
        CustomRoutingDataSource.clearCurrentDataSource();
        return byId;
    }

    @Transactional(readOnly = true)
    public Page<MnoMapping> searchMnoMapping(String searchTerm, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Pageable pageRequest = createPageRequest(page, limit, sort, order);
        Page<MnoMapping> pageResult = this.mnoRepository.findAll(searchTerm, pageRequest);
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

    public MnoMapping findByName(String operator) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        MnoMapping byMno = this.mnoRepository.findByMno(operator);
        CustomRoutingDataSource.clearCurrentDataSource();
        return byMno;
    }


}
