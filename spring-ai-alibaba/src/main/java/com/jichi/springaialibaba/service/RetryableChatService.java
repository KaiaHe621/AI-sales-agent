package com.jichi.springaialibaba.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class RetryableChatService {

    private static final Logger log = LoggerFactory.getLogger(RetryableChatService.class);

    private final ChatClient chatClient;

    public RetryableChatService(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 遇到 429 或 5xx 错误时，最多重试 3 次，每次等待时间翻倍（1s、2s、4s）
     */
    @Retryable(
            retryFor = {
                    HttpServerErrorException.class,
                    HttpClientErrorException.TooManyRequests.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String chatWithRetry(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 所有重试都失败后的兜底处理，方法签名中异常类型必须和 @Retryable 匹配
     */
    @Recover
    public String recover(Exception e, String message) {
        log.error("AI 调用重试 3 次仍失败: {}", e.getMessage());
        return "抱歉，AI 服务暂时不可用，请稍后重试。";
    }
}