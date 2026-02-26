package com.hybrid9.pg.Lipanasi.serviceImpl;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.AppUser;
import com.hybrid9.pg.Lipanasi.repositories.UserRepository;
import com.hybrid9.pg.Lipanasi.services.AppUserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AppUserServiceImpl implements AppUserService, UserDetailsService {
    private UserRepository userRepository;
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        AppUser user = userRepository.findByUsername(username);
        if(user == null){
            throw new UsernameNotFoundException("User "+user.getUsername()+" was not found");
        }else{
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            user.getRoles().stream().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));
            CustomRoutingDataSource.clearCurrentDataSource();
            return new org.springframework.security.core.userdetails.User(user.getUsername(),user.getPassword(),authorities);
        }
    }
    @Transactional
    @Override
    public AppUser addUser(AppUser user) {
        return null;
    }
    @Transactional(readOnly = true)
    @Override
    public AppUser findUserByUsername(String username) {
        return null;
    }
    @Transactional(readOnly = true)
    @Override
    public Page<AppUser> findAll(int page, int size, String sort, String order) {
        return null;
    }
    @Transactional(readOnly = true)
    @Override
    public Optional<AppUser> findByPhoneNumber(String phoneNumber) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<AppUser> user = userRepository.findByPhoneNumber(phoneNumber);
        CustomRoutingDataSource.clearCurrentDataSource();
        return user;
    }
}
