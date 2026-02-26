package com.hybrid9.pg.Lipanasi.services.config;

import com.hybrid9.pg.Lipanasi.entities.ApiToken;
import com.hybrid9.pg.Lipanasi.repositories.config.ApiTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ApiTokenService {

    private ApiTokenRepository apiTokenRepository;

    public ApiTokenService(ApiTokenRepository apiTokenRepository) {
        this.apiTokenRepository = apiTokenRepository;
    }

    @Transactional
    public ApiToken newApiToken(ApiToken apiToken) {
        return this.apiTokenRepository.save(apiToken);
    }
    @Transactional
    public void addApiToken(ApiToken apiToken) {
        this.apiTokenRepository.save(apiToken);
    }
    @Transactional
    public ApiToken updateApiToken(ApiToken apiToken) {
        return this.apiTokenRepository.save(apiToken);
    }
    @Transactional
    public void update(ApiToken apiToken) {
        this.apiTokenRepository.save(apiToken);
    }
    @Transactional(readOnly = true)
    public ApiToken findById(long id) {
        return this.apiTokenRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public ApiToken findByType(String type) {
        return this.apiTokenRepository.findByType(type);
    }
    @Transactional(readOnly = true)
    public Page<ApiToken> findAll(int page, int limit, String sort, String order) {
        if (order.equalsIgnoreCase("asc")) {
            return this.apiTokenRepository.findAll(PageRequest.of(page, limit, Sort.by(sort).ascending()));
        } else {
            return this.apiTokenRepository.findAll(PageRequest.of(page, limit, Sort.by(sort).descending()));
        }
    }
    @Transactional(readOnly = true)
    public Page<ApiToken> findAll(String type, String headerKey, String token, int page, int limit, String sort, String order) {
        if (order.equalsIgnoreCase("asc")) {
            return this.apiTokenRepository.findAllByTypeAndHeaderKeyAndToken(type, headerKey, token, PageRequest.of(page, limit, Sort.by(sort).ascending()));
        } else {
            return this.apiTokenRepository.findAllByTypeAndHeaderKeyAndToken(type, headerKey, token, PageRequest.of(page, limit, Sort.by(sort).descending()));
        }
    }
}
