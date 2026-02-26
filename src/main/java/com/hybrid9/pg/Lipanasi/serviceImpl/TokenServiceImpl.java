package com.hybrid9.pg.Lipanasi.serviceImpl;

import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.auth.Token;
import com.hybrid9.pg.Lipanasi.repositories.auth.TokenRepository;
import com.hybrid9.pg.Lipanasi.services.auth.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class TokenServiceImpl implements TokenService {
    private TokenRepository tokenRepository;
    @Transactional(readOnly = true)
    @Override
    public Token findByRefreshToken(String refreshToken) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Token token = this.tokenRepository.findByRefreshToken(refreshToken);
        CustomRoutingDataSource.clearCurrentDataSource();
        return token;
    }

    @Override
    @Transactional
    public void addToken(Token token) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.tokenRepository.save(token);
        CustomRoutingDataSource.clearCurrentDataSource();
    }

    @Override
    @Transactional
    public void updateToken(Token token) {
     CustomRoutingDataSource.setCurrentDataSource("primary");
        this.tokenRepository.save(token);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public String getRefreshToken(String userId) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Token token = this.tokenRepository.findByUserId(userId);
        CustomRoutingDataSource.clearCurrentDataSource();
        return token.getRefreshToken();

       // return Optional.ofNullable(this.tokenRepository.findByUserId(userId)).map(Token::getRefreshToken).orElse(null);
    }
    @Transactional
    @Override
    public Token removeOldTokens(String username) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
       return Optional.ofNullable(this.tokenRepository.findByUserId(username)).map(token -> {
           this.tokenRepository.deleteByUserId(username);
           CustomRoutingDataSource.clearCurrentDataSource();
           return token;
       }).orElse(null);

    }
    @Transactional
    @Override
    public void removeToken(String username) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        this.tokenRepository.deleteByUserId(username);
        CustomRoutingDataSource.clearCurrentDataSource();
    }
    @Transactional(readOnly = true)
    @Override
    public Token findByUserId(String username) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Token token = this.tokenRepository.findByUserId(username);
        CustomRoutingDataSource.clearCurrentDataSource();
        return token;
    }
    @Transactional(readOnly = true)
    @Override
    public Token findByUserIdAndRefreshToken(String username, String refreshToken) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Token token = this.tokenRepository.findByUserIdAndRefreshToken(username,refreshToken);
        CustomRoutingDataSource.clearCurrentDataSource();
        return token;

    }
}
