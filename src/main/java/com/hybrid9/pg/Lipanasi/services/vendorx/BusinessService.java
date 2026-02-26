package com.hybrid9.pg.Lipanasi.services.vendorx;

import com.hybrid9.pg.Lipanasi.entities.vendorx.Business;

public interface BusinessService {
    Business findBusinessByName(String name);

    Business registerBusiness(Business business);

    Business updateBusiness(Business business);
}
