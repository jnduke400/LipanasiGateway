package com.hybrid9.pg.Lipanasi.serviceImpl;


import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.ApiCredential;
import com.hybrid9.pg.Lipanasi.repositories.ApiCredentialRepository;
import com.hybrid9.pg.Lipanasi.services.ApiCredentialService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ApiCredentialServiceImpl implements ApiCredentialService {
    private ApiCredentialRepository apiCredentialRepository;

    @Transactional
    @Override
    public ApiCredential newApiCredential(ApiCredential apiCredential) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        apiCredential = this.apiCredentialRepository.save(apiCredential);
        CustomRoutingDataSource.clearCurrentDataSource();
        return apiCredential;
    }
    @Transactional
    @Override
    public void addApiCredential(ApiCredential apiCredential) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.apiCredentialRepository.save(apiCredential);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional
    @Override
    public ApiCredential updateApiCredential(ApiCredential apiCredential) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        apiCredential = this.apiCredentialRepository.save(apiCredential);
        CustomRoutingDataSource.clearCurrentDataSource();
        return apiCredential;
    }
    @Transactional
    @Override
    public void update(ApiCredential apiCredential) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.apiCredentialRepository.save(apiCredential);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public ApiCredential findById(long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        ApiCredential apiCredential = this.apiCredentialRepository.findById(id);
        CustomRoutingDataSource.clearCurrentDataSource();
        return apiCredential;
    }
    @Transactional(readOnly = true)
    @Override
    public ApiCredential findByType(String type) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        ApiCredential apiCredential = this.apiCredentialRepository.findByType(type);
        CustomRoutingDataSource.clearCurrentDataSource();
        return apiCredential;
    }
    @Transactional(readOnly = true)
    @Override
    public Page<ApiCredential> findAll(int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equalsIgnoreCase("asc")) {
            Page<ApiCredential> pageResult = this.apiCredentialRepository.findAll(PageRequest.of(page, limit, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<ApiCredential> pageResult = this.apiCredentialRepository.findAll(PageRequest.of(page, limit, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }
    }
    @Transactional(readOnly = true)
    @Override
    public Page<ApiCredential> findAll(String type, String url, String userKey, String passKey, String username, String password, int page, int limit, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equalsIgnoreCase("asc")) {
            Page<ApiCredential> pageResult = this.apiCredentialRepository.findAllByTypeAndUrlAndUserKeyAndPassKeyAndUsernameAndPassword(type, url, userKey, passKey, username, password, PageRequest.of(page, limit, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<ApiCredential> pageResult = this.apiCredentialRepository.findAllByTypeAndUrlAndUserKeyAndPassKeyAndUsernameAndPassword(type, url, userKey, passKey, username, password, PageRequest.of(page, limit, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }
    }
}
