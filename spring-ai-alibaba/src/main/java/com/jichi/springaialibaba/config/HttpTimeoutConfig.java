package com.jichi.springaialibaba.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class HttpTimeoutConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> {
            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(
                            ConnectionConfig.custom()
                                    .setConnectTimeout(Timeout.ofSeconds(10))  // 建立连接超时
                                    .setSocketTimeout(Timeout.ofSeconds(60))   // 读取数据超时，生成长文本要设长一些
                                    .build())
                    .build();

            var httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            builder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        };
    }
}