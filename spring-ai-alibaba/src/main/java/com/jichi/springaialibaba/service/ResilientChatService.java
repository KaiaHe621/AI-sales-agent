package com.jichi.springaialibaba.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ResilientChatService {

    private static final Logger log = LoggerFactory.getLogger(ResilientChatService.class);

    private final ChatClient primaryChatClient;
    private final ChatClient backupChatClient;

    public ResilientChatService(
            @Qualifier("primaryChatClient") ChatClient primaryChatClient,
            @Qualifier("backupChatClient") ChatClient backupChatClient) {
        this.primaryChatClient = primaryChatClient;
        this.backupChatClient = backupChatClient;
    }

    /**
     * 先重试，重试都失败后触发熔断，熔断后走降级方法
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "fallbackChat")
    @Retry(name = "aiService")
    public String chat(String message) {
        return primaryChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 降级方法：主模型熔断时切换到备用模型
     * 签名必须和原方法一致，最后加一个 Throwable 参数
     */
    public String fallbackChat(String message, Throwable throwable) {
        log.warn("主模型不可用（{}），切换备用模型", throwable.getMessage());
        try {
            return backupChatClient.prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("备用模型也不可用", e);
            return "AI 服务暂时不可用，请稍后重试。如有紧急需求，请联系客服。";
        }
    }
}