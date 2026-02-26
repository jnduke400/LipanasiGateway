package com.hybrid9.pg.Lipanasi.configs.gw;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(PaymentGatewayProperties.class)
public class PaymentGatewayConfig {

    private final PaymentGatewayProperties properties;

    public PaymentGatewayConfig(PaymentGatewayProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestTemplate paymentGatewayRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configure timeouts
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectionTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        restTemplate.setRequestFactory(requestFactory);

        // Add error handling
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                // We'll handle HTTP errors ourselves in the service
                return false;
            }
        });

        return restTemplate;
    }
}
