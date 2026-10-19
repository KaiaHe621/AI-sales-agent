package com.jichi.ragkb.service;

import com.jichi.ragkb.dto.RagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 查询结果缓存服务。
 * 相同问题 + 相同知识库 → 缓存 10 分钟，不重复调模型。
 *
 * 适用场景：同一部门内，相同问题被多人反复问（FAQ 类场景命中率高）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "rag:query:";
    private static final Duration QUERY_TTL = Duration.ofMinutes(10);

    /**
     * 查询缓存。
     */
    public RagResponse getFromCache(String question, List<Long> kbIds) {
        String key = buildKey(question, kbIds);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof RagResponse resp) {
            log.info("[QueryCache] 命中缓存：question={}", question.substring(0, Math.min(30, question.length())));
            return resp;
        }
        return null;
    }

    /**
     * 写入缓存。
     */
    public void putToCache(String question, List<Long> kbIds, RagResponse response) {
        // 无效答案不缓存（notFound 的结果可能因为文档更新而变化）
        if (response.isNotFound()) return;

        String key = buildKey(question, kbIds);
        redisTemplate.opsForValue().set(key, response, QUERY_TTL);
        log.debug("[QueryCache] 写入缓存：key={}", key);
    }

    private String buildKey(String question, List<Long> kbIds) {
        // 排序 kbIds 保证相同知识库集合的 key 一致
        List<Long> sortedIds = kbIds.stream().sorted().toList();
        return CACHE_PREFIX + toMd5(question + ":" + sortedIds);
    }

    private String toMd5(String text) {
        try {
            var md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}