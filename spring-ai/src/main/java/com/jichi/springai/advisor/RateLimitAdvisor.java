package com.jichi.springai.advisor;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitAdvisor implements CallAdvisor {

    // 每个用户每秒最多 2 次调用
    private static final double PERMITS_PER_SECOND = 2.0;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 从 context 里拿 userId（由 Controller 通过 .advisors(a -> a.param("userId", userId)) 传入）
        String userId = (String) request.context()
                .getOrDefault("userId", "anonymous");

        RateLimiter limiter = limiters.computeIfAbsent(userId,
                k -> RateLimiter.create(PERMITS_PER_SECOND));

        // tryAcquire 非阻塞，拿不到直接抛异常
        if (!limiter.tryAcquire()) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }

        return chain.nextCall(request);
    }

    @Override
    public String getName() {
        return "RateLimitAdvisor";
    }

    @Override
    public int getOrder() {
        return 10; // 在日志之后、记忆之前执行
    }
    
}