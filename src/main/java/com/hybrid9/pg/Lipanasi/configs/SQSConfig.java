package com.hybrid9.pg.Lipanasi.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.sqs")
public class SQSConfig {
    private String accessKey;
    private String secretKey;
    private String region;
    private QueueConfig queue;
    private Integer maxMessagesPerPoll;
    private Integer visibilityTimeout;
    private Integer waitTimeSeconds;

    @Data
    public static class QueueConfig {
        private String name;
        private String url;
    }
}
