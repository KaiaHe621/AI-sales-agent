package com.jichi.voiceshopping.memory;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortTermMemory {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    @Value("${voice-shopping.session.ttl-minutes}")
    private int ttlMinutes;

    @Value("${voice-shopping.session.max-history-turns}")
    private int maxTurns;

    private String key(String sessionId) {
        return "vs:short_memory:" + sessionId;
    }

    @SneakyThrows
    public void append(String sessionId, Turn turn) {
        String json = mapper.writeValueAsString(turn);
        redis.opsForList().rightPush(key(sessionId), json);
        redis.opsForList().trim(key(sessionId), -maxTurns, -1);
        redis.expire(key(sessionId), Duration.ofMinutes(ttlMinutes));
    }

    @SneakyThrows
    public List<Turn> recent(String sessionId, int limit) {
        List<String> raws = redis.opsForList().range(key(sessionId), -limit, -1);
        if (raws == null) return List.of();
        List<Turn> turns = new java.util.ArrayList<>();
        for (String r : raws) turns.add(mapper.readValue(r, Turn.class));
        return turns;
    }

    /**
     * 清理
     */
    public void clear(String sessionId) {
        redis.delete(key(sessionId));
    }

    public record Turn(String role, String agent, String text, long ts) {
    }
}