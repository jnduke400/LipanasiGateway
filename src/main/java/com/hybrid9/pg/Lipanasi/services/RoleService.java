package com.hybrid9.pg.Lipanasi.services;


import com.hybrid9.pg.Lipanasi.entities.Role;

import java.util.Optional;

public interface RoleService {
    Role createRole(String roleName);
    Optional<Role> findByName(String roleAdmin);
    Role findById(long id);
}
