package com.hybrid9.pg.Lipanasi.configs;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class HttpClientConfig {

    @Bean(name = "airtelHttpClient")
    public HttpClient airtelHttpClient() {
        return createHttpClient(20, 10, 60000, 60000);
    }

    @Bean(name = "mixxHttpClient")
    public HttpClient mixxHttpClient() {
        return createHttpClient(20, 10, 120000, 120000);
    }

    @Bean(name = "mpesaHttpClient")
    public HttpClient mpesaHttpClient() {
        return createHttpClient(20, 10, 120000, 120000);
    }

    @Bean(name = "halopesaHttpClient")
    public HttpClient halopesaHttpClient() {
        return createHttpClient(20, 10, 120000, 120000);
    }

    @Bean(name = "amTqsHttpClient")
    public HttpClient amTqsHttpClient() {
        return createHttpClient(20, 10, 120000, 120000);
    }

    @Bean(name = "crdbHttpClient")
    public HttpClient crdbHttpClient() {
        return createHttpClient(20, 10, 120000, 120000);
    }

    @Bean(name = "softnetHttpClient")
    public HttpClient softnetHttpClient() {
        return createHttpClient(20, 10, 120000, 120000);
    }

    private HttpClient createHttpClient(int maxConnections, int maxConnectionsPerRoute,
                                        int connectTimeout, int socketTimeout) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(30000)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy((response, context) -> 30000) // Keep-alive for 30 seconds
                .build();
    }
}
