package com.jichi.prompt.service;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserRateLimiter {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public boolean tryAcquire(String userId) {
        RateLimiter limiter = limiters.computeIfAbsent(userId,
                id -> RateLimiter.create(20.0 / 60));  // 20次/分钟
        return limiter.tryAcquire();
    }
}