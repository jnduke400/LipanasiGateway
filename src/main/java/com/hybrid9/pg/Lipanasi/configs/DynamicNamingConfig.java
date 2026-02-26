package com.hybrid9.pg.Lipanasi.configs;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamicNamingConfig {
    @Bean
    public PhysicalNamingStrategy physicalNamingStrategy() {
        return new MultiTableDynamicNamingStrategy();
    }
}