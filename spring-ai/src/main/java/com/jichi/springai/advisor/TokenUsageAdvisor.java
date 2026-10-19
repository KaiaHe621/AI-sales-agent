package com.jichi.springai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class TokenUsageAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    // 内存中统计各用户累计 Token，生产环境换成数据库或 Redis
    private final ConcurrentHashMap<String, LongAdder> userTokenCount = new ConcurrentHashMap<>();

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        // 从响应中拿 Token 用量
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null
                && chatResponse.getMetadata() != null
                && chatResponse.getMetadata().getUsage() != null) {

            var usage = chatResponse.getMetadata().getUsage();
            String userId = (String) request.context()
                    .getOrDefault("userId", "anonymous");

            long total = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0L;

            // 累计该用户的 Token 用量
            userTokenCount.computeIfAbsent(userId, k -> new LongAdder()).add(total);

            log.info("[Token统计] userId={}, 本次 prompt={}, completion={}, total={}, 累计={}",
                    userId,
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    total,
                    userTokenCount.get(userId).sum());
        }

        return response;
    }

    /** 查询某用户的累计 Token 消耗（供 Controller 调用） */
    public long getTotalTokens(String userId) {
        LongAdder adder = userTokenCount.get(userId);
        return adder != null ? adder.sum() : 0L;
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }

    @Override
    public int getOrder() {
        return 20; // 最后执行，确保拿到完整响应
    }
}