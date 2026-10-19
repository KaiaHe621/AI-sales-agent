package com.jichi.ragkb.service;

import com.jichi.ragkb.security.UserContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * RAG 查询的 Token 消耗统计器。
 *
 * 双写策略：
 * 1. Micrometer Counter —— 全局指标，供 Prometheus / Grafana 监控使用，重启归零
 * 2. Redis Hash —— 按用户维度持久化，重启不丢失，供前端监控面板展示
 *
 * 注意：必须用 StringRedisTemplate，不能用自定义的 RedisTemplate<String, Object>。
 * 因为 GenericJackson2JsonRedisSerializer 和 HINCRBY 写入的原生数字格式不兼容：
 * increment() 写入 "150"，但 JSON 反序列化器期望 ["java.lang.Long", 150]，读取时会失败。
 * StringRedisTemplate 全部使用 StringRedisSerializer，读写一致。
 *
 * Redis Key 格式：rag:token-stats:{userId}
 * Hash Fields：embeddingTokens / contextTokens / generationTokens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenMetrics {

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_KEY_PREFIX = "rag:token-stats:";

    private Counter embeddingTokenCounter;
    private Counter contextTokenCounter;
    private Counter generationTokenCounter;

    @PostConstruct
    public void init() {
        embeddingTokenCounter = Counter.builder("rag.tokens.embedding")
                .description("Embedding 消耗的 Token 总数")
                .register(meterRegistry);

        contextTokenCounter = Counter.builder("rag.tokens.context")
                .description("传入模型的 Context Token 总数")
                .register(meterRegistry);

        generationTokenCounter = Counter.builder("rag.tokens.generation")
                .description("模型生成消耗的 Token 总数")
                .register(meterRegistry);
    }

    public void recordEmbeddingTokens(int tokens) {
        log.info("[TokenMetrics] recordEmbeddingTokens={}", tokens);
        embeddingTokenCounter.increment(tokens);
        incrementRedis("embeddingTokens", tokens);
    }

    public void recordContextTokens(int tokens) {
        log.info("[TokenMetrics] recordContextTokens={}", tokens);
        contextTokenCounter.increment(tokens);
        incrementRedis("contextTokens", tokens);
    }

    public void recordGenerationTokens(int tokens) {
        log.info("[TokenMetrics] recordGenerationTokens={}", tokens);
        generationTokenCounter.increment(tokens);
        incrementRedis("generationTokens", tokens);
    }

    /** 从 Redis 读取指定用户的 Token 统计 */
    public long getUserTokens(Long userId, String field) {
        String val = (String) stringRedisTemplate.opsForHash()
                .get(REDIS_KEY_PREFIX + userId, field);
        log.info("[TokenMetrics] getUserTokens: userId={}, field={}, val={}", userId, field, val);
        if (val != null) {
            try { return Long.parseLong(val); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private void incrementRedis(String field, int delta) {
        try {
            Long userId = UserContext.getUserId();
            String key = REDIS_KEY_PREFIX + userId;
            log.info("[TokenMetrics] incrementRedis: key={}, field={}, delta={}", key, field, delta);
            stringRedisTemplate.opsForHash().increment(key, field, delta);
        } catch (Exception e) {
            log.error("[TokenMetrics] Redis 写入失败：{}", e.getMessage(), e);
        }
    }
}