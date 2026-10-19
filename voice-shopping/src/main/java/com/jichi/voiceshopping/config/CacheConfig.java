package com.jichi.voiceshopping.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    /**
     * 给 "embed" 这个 cache 单独配 7 天 TTL，其它 cache 走 application.yml 里的默认 30 分钟
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer embedCacheTtl() {
        return builder -> builder.withCacheConfiguration("embed",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofDays(7)));
    }

}