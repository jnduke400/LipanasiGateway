package com.hybrid9.pg.Lipanasi.services.auth;


import com.hybrid9.pg.Lipanasi.entities.auth.Token;

public interface TokenService {
    Token findByRefreshToken(String refreshToken);

    void addToken(Token token);

    void updateToken(Token token);

    String getRefreshToken(String userId);

    Token removeOldTokens(String username);

    void removeToken(String username);

    Token findByUserId(String username);

    Token findByUserIdAndRefreshToken(String username, String refreshToken);
}
