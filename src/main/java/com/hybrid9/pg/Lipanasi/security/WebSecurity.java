package com.hybrid9.pg.Lipanasi.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class WebSecurity {
    @Autowired
    JwtToUserConverter jwtToUserConverter;
    @Autowired
    KeyUtils keyUtils;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserDetailsManager userDetailsManager;

    @Bean
    CORSFilter corsFilter(){
        return new CORSFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/*")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/inbounds/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/mno/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/senderids/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/pay/bill/halopesa/payment/")).permitAll()
                        /*.requestMatchers(new AntPathRequestMatcher("api/v1/collection/init/")).authenticated()*/
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/collection/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/collection/mpesa/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cngtz/collection/tigopesa/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cngtz/collection/prod/airtelmoney/payment")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cngtz/collection/orange-congo/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cngtz/collection/mpesa-congo/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/cngtz/collection/airtelmoney-congo/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/aitelMoney/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/pay/bill/mixx_by_yas/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/mpesa/payment/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/mpesa/payment/prod/")).permitAll()
                        /*.requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/airtelMoney/enquiry/prod/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/airtelMoney/payment/prod/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/airtelMoney/validate/prod/")).permitAll()*/
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/airtelMoney/enquiry/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/airtelMoney/payment/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("api/v1/pay/bill/airtelMoney/validate/**")).permitAll()

                        .requestMatchers(new AntPathRequestMatcher("/api/v1/order/create")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/order/status/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/payments/process")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/payments/status/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/operator/**")).permitAll()

                        .requestMatchers(new AntPathRequestMatcher("/api/v1/deposits/**")).permitAll()

                        .requestMatchers(new AntPathRequestMatcher("/api/v1/crdb/payment/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/crdb/payment/process")).permitAll()

                        .requestMatchers(new AntPathRequestMatcher("/api/v1/plugin-analytics/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf().disable()
                .cors().disable()
                .httpBasic().disable()
                .oauth2ResourceServer((oauth2) ->
                        oauth2.jwt((jwt) -> jwt.jwtAuthenticationConverter(jwtToUserConverter))
                )
                .addFilterBefore(corsFilter(), SessionManagementFilter.class)
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                );
        return http.build();
    }

    @Bean
    @Primary
    JwtDecoder jwtAccessTokenDecoder() {
        return NimbusJwtDecoder.withPublicKey(keyUtils.getAccessTokenPublicKey()).build();
    }

    @Bean
    @Primary
    JwtEncoder jwtAccessTokenEncoder() {
        JWK jwk = new RSAKey
                .Builder(keyUtils.getAccessTokenPublicKey())
                .privateKey(keyUtils.getAccessTokenPrivateKey())
                .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    @Qualifier("jwtRefreshTokenDecoder")
    JwtDecoder jwtRefreshTokenDecoder() {
        return NimbusJwtDecoder.withPublicKey(keyUtils.getRefreshTokenPublicKey()).build();
    }

    @Bean
    @Qualifier("jwtRefreshTokenEncoder")
    JwtEncoder jwtRefreshTokenEncoder() {
        JWK jwk = new RSAKey
                .Builder(keyUtils.getRefreshTokenPublicKey())
                .privateKey(keyUtils.getRefreshTokenPrivateKey())
                .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    @Qualifier("jwtRefreshTokenAuthProvider")
    JwtAuthenticationProvider jwtRefreshTokenAuthProvider() {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtRefreshTokenDecoder());
        provider.setJwtAuthenticationConverter(jwtToUserConverter);
        return provider;
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsManager);
        return provider;
    }
}
