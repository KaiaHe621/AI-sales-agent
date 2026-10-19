package com.jichi.salesAgent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SalesQueryCacheService {

    private final SalesQueryService queryService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${sales-agent.cache.query-ttl-seconds:300}")
    private long cacheTtlSeconds;

    public BigDecimal queryTotalAmountCached(Long regionId, LocalDate start, LocalDate end) {
        String cacheKey = String.format("total_amount:%s:%s:%s",
                regionId != null ? regionId : "all", start, end);

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new BigDecimal(cached.toString());
        }

        BigDecimal result = queryService.queryTotalAmount(regionId, start, end);
        redisTemplate.opsForValue().set(cacheKey, result.toPlainString(),
                Duration.ofSeconds(cacheTtlSeconds));
        return result;
    }
}