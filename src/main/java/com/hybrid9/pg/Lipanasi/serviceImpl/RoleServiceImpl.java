package com.hybrid9.pg.Lipanasi.serviceImpl;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.Role;
import com.hybrid9.pg.Lipanasi.repositories.RoleRepository;
import com.hybrid9.pg.Lipanasi.services.RoleService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    @Transactional
    @Override
    public Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }
    @Transactional(readOnly = true)
    @Override
    public Role findById(long id) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Role role = this.roleRepository.findById(id).orElse(null);
        CustomRoutingDataSource.clearCurrentDataSource();
        return role;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<Role> findByName(String roleAdmin) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Role role = this.roleRepository.findByName(roleAdmin).orElse(null);
        CustomRoutingDataSource.clearCurrentDataSource();
        return Optional.ofNullable(role);
    }
}
