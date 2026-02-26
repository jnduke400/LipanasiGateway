package com.hybrid9.pg.Lipanasi.serviceImpl;



import com.hybrid9.pg.Lipanasi.repositories.ApiPostHeaderRepository;
import com.hybrid9.pg.Lipanasi.services.ApiPostHeaderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApiPostHeaderServiceImpl implements ApiPostHeaderService {
    private ApiPostHeaderRepository apiPostHeaderRepository;

}
