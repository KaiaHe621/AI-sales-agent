package com.jichi.voiceshopping.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.voiceshopping.dto.SessionScope;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SessionScopeCache {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringRedisTemplate redis;

    @SneakyThrows
    public void put(String sessionId, SessionScope scope) {
        redis.opsForValue().set(key(sessionId), MAPPER.writeValueAsString(scope), TTL);
    }

    public SessionScope get(String sessionId) {
        String json = redis.opsForValue().get(key(sessionId));
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, SessionScope.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String key(String sessionId) {
        return "vs:scope:" + sessionId;
    }
}