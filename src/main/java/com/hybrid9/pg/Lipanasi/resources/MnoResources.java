package com.hybrid9.pg.Lipanasi.resources;



import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.PrefixServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
@AllArgsConstructor
public class MnoResources {
    private PrefixServiceImpl prefixService;
    public String getInternationalMnoDialCode(String msisdn){
        List<MnoPrefix> prefixList = this.prefixService.findAllPrefix();
        String mnoPrefix = null;
        Predicate<MnoPrefix> oneCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,1));
        Predicate<MnoPrefix> twoCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,2));
        Predicate<MnoPrefix> threeCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,3));
        Predicate<MnoPrefix> fourCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,4));
        if(prefixList.stream().anyMatch(fourCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,4);
        }else if(prefixList.stream().anyMatch(threeCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,3);
        }else if(prefixList.stream().anyMatch(twoCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,2);
        }else if(prefixList.stream().anyMatch(oneCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,1);
        }

        return mnoPrefix;
    }

    public String getInternationalMnoPrefix(String msisdn){
        List<MnoPrefix> prefixList = this.prefixService.findAllPrefix();
        String mnoPrefix = null;
        Predicate<MnoPrefix> oneCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,1));
        Predicate<MnoPrefix> twoCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,2));
        Predicate<MnoPrefix> threeCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,3));
        Predicate<MnoPrefix> fourCountryCodeLength = p -> p.getDialCode().equalsIgnoreCase(msisdn.substring(0,4));
        if(prefixList.stream().anyMatch(fourCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,6);
        }else if(prefixList.stream().anyMatch(threeCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,5);
        }else if(prefixList.stream().anyMatch(twoCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,4);
        }else if(prefixList.stream().anyMatch(oneCountryCodeLength)){
            mnoPrefix = msisdn.substring(0,3);
        }

        return mnoPrefix;
    }
}
