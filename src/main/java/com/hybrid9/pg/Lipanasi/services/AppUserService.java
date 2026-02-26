package com.hybrid9.pg.Lipanasi.services;


import com.hybrid9.pg.Lipanasi.entities.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface AppUserService {

    AppUser addUser(AppUser user);
    AppUser findUserByUsername(String username);

    Page<AppUser> findAll(int page, int size, String sort, String order);

    Optional<AppUser> findByPhoneNumber(String phoneNumber);
}
