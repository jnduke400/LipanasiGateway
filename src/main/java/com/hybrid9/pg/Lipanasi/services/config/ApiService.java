package com.hybrid9.pg.Lipanasi.services.config;



import com.hybrid9.pg.Lipanasi.entities.Api;
import com.hybrid9.pg.Lipanasi.repositories.config.ApiRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ApiService {

    private ApiRepository apiRepository;

    public ApiService(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Transactional
    public Api newApi(Api api) {
        return this.apiRepository.save(api);
    }
    @Transactional
    public void addApi(Api api) {
        this.apiRepository.save(api);
    }
    @Transactional
    public Api updateApi(Api api) {
        return this.apiRepository.save(api);
    }
    @Transactional
    public void update(Api api) {
        this.apiRepository.save(api);
    }
    @Transactional(readOnly = true)
    public Api findById(long id) {
        return this.apiRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public Api findByTypeAndHeaderKey(String type, String headerKey) {
        return this.apiRepository.findByTypeAndHeaderKey(type, headerKey);
    }
    @Transactional(readOnly = true)
    public Page<Api> findAll(int page, int limit, String sort, String order) {
        if (order.equalsIgnoreCase("asc")) {
            return this.apiRepository.findAll(PageRequest.of(page, limit, Sort.by(sort).ascending()));
        } else {
            return this.apiRepository.findAll(PageRequest.of(page, limit, Sort.by(sort).descending()));
        }
    }
    @Transactional(readOnly = true)
    public Page<Api> findAll(String type, int page, int limit, String sort, String order) {
        if (order.equalsIgnoreCase("asc")) {
            return this.apiRepository.findAllByType(type, PageRequest.of(page, limit, Sort.by(sort).ascending()));
        } else {
            return this.apiRepository.findAllByType(type, PageRequest.of(page, limit, Sort.by(sort).descending()));
        }
    }
    @Transactional(readOnly = true)
    public Page<Api> findAll(String type, String url, String headerKey, String headerValue, int page, int limit, String sort, String order) {
        if (order.equalsIgnoreCase("asc")) {
            return this.apiRepository.findAllByTypeAndUrlAndHeaderKeyAndHeaderValue(type, url, headerKey, headerValue, PageRequest.of(page, limit, Sort.by(sort).ascending()));
        } else {
            return this.apiRepository.findAllByTypeAndUrlAndHeaderKeyAndHeaderValue(type, url, headerKey, headerValue, PageRequest.of(page, limit, Sort.by(sort).descending()));
        }
    }
}
