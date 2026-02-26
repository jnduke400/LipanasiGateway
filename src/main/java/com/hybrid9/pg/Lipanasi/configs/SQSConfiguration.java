package com.hybrid9.pg.Lipanasi.configs;//package com.gtl.smpp.smpprouter.configs;
//
//import org.apache.camel.component.aws2.sqs.MessageGroupIdStrategy;
//import org.apache.camel.component.aws2.sqs.Sqs2Component;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.sqs.SqsClient;
//
//
//@Configuration
//public class SQSConfiguration {
//
//    @Bean
//    public SqsClient sqsClient(SQSConfig sqsConfig) {
//        return SqsClient.builder()
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(
//                                sqsConfig.getAccessKey(),
//                                sqsConfig.getSecretKey()
//                        )
//                ))
//                .region(Region.of(sqsConfig.getRegion()))
//                .build();
//    }
//
//    @Bean
//    public Sqs2Component sqs2Component(SqsClient sqsClient) {
//        Sqs2Component component = new Sqs2Component();
//        component.getConfiguration().setAmazonSQSClient(sqsClient);
//        return component;
//    }
//
//}
