package com.hybrid9.pg.Lipanasi.repositories;


import com.hybrid9.pg.Lipanasi.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    @Query(value = "SELECT * FROM c2b_users u WHERE u.username = :username",nativeQuery = true)
    AppUser findByUsername(@Param("username") String username);

    boolean existsByUsername(String username);

    Optional<AppUser> findByPhoneNumber(String phoneNumber);
}
