package com.hybrid9.pg.Lipanasi.services;

import com.hybrid9.pg.Lipanasi.entities.ApiCredential;
import org.springframework.data.domain.Page;

public interface ApiCredentialService {

    public ApiCredential newApiCredential(ApiCredential apiCredential) ;
    public void addApiCredential(ApiCredential apiCredential);

    public ApiCredential updateApiCredential(ApiCredential apiCredential);


    public void update(ApiCredential apiCredential);

    public ApiCredential findById(long id);

    public ApiCredential findByType(String type) ;

    public Page<ApiCredential> findAll(int page, int limit, String sort, String order) ;


    public Page<ApiCredential> findAll(String type, String url, String userKey, String passKey, String username, String password, int page, int limit, String sort, String order) ;

}
