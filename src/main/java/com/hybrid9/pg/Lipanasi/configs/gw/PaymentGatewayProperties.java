package com.hybrid9.pg.Lipanasi.configs.gw;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.gateway")
@Data
public class PaymentGatewayProperties {
    private String endpoint;
    private String apiKey;
    private String merchantId;
    private String returnUrl;
    private int connectionTimeout = 5000;
    private int readTimeout = 30000;
}
