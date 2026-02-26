package com.hybrid9.pg.Lipanasi.configs;

import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class CamelTransactionConfig {

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Creates a propagation required transaction policy for Camel routes
     */
    @Bean(name = "PROPAGATION_REQUIRED")
    public SpringTransactionPolicy propagationRequired() {
        SpringTransactionPolicy policy = new SpringTransactionPolicy();
        policy.setTransactionManager(transactionManager);
        policy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return policy;
    }

    /**
     * Optional: Create a transaction template that can be used for programmatic transaction management
     */
    @Bean
    public TransactionTemplate transactionTemplate() {
        TransactionTemplate template = new TransactionTemplate();
        template.setTransactionManager(transactionManager);
        return template;
    }
}
