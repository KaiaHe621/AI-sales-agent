package com.jichi.voiceshopping.service;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PendingOrderStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public record PendingOrder(
            String sessionId,
            Long userId,
            Long merchantId,
            Long productId,
            String productName,
            String skuCode,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount
    ) {}

    private String key(String sessionId) {
        return "vs:pending_order:" + sessionId;
    }

    @SneakyThrows
    public void put(PendingOrder o) {
        redis.opsForValue().set(key(o.sessionId()),
                mapper.writeValueAsString(o), Duration.ofMinutes(10));
    }

    @SneakyThrows
    public PendingOrder get(String sessionId) {
        String raw = redis.opsForValue().get(key(sessionId));
        return raw == null ? null : mapper.readValue(raw, PendingOrder.class);
    }

    public void remove(String sessionId) {
        redis.delete(key(sessionId));
    }
}