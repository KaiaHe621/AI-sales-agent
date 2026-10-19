package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.IntentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IntentCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public IntentResult get(String sessionId, String utterance) {
        String key = buildKey(sessionId, utterance);
        String raw = redis.opsForValue().get(key);
        if (raw == null) return null;
        try { return mapper.readValue(raw, IntentResult.class); }
        catch (Exception e) { return null; }
    }

    public void put(String sessionId, String utterance, IntentResult result) {
        String key = buildKey(sessionId, utterance);
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(result), Duration.ofMinutes(5));
        } catch (Exception ignored) {}
    }

    private String buildKey(String sessionId, String utterance) {
        return "vs:intent_cache:" + sessionId + ":" + UtteranceFingerprint.compute(utterance);
    }
}