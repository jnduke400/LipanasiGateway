package com.hybrid9.pg.Lipanasi.configs;

import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Configuration
public class SSLConfig {

    @Bean(name = "noopSslContext")
    public SSLContextParameters sslContextParameters() {
        SSLContextParameters sslContextParameters = new SSLContextParameters();

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setTrustManager(new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        });

        sslContextParameters.setTrustManagers(trustManagersParameters);
        return sslContextParameters;
    }
}
