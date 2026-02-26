package com.hybrid9.pg.Lipanasi.repositories.auth;




import com.hybrid9.pg.Lipanasi.entities.auth.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Token findByRefreshToken(String refreshToken);

    Token findByUserId(String userId);

    Token deleteByUserId(String username);

    Token findByUserIdAndRefreshToken(String username, String refreshToken);
}