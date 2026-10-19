package com.jichi.springaialibaba.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class ManualRetryChatService {

    private static final Logger log = LoggerFactory.getLogger(ManualRetryChatService.class);

    private final ChatClient chatClient;

    public ManualRetryChatService(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chatWithManualRetry(String message) {
        int maxAttempts = 3;
        long delayMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chatClient.prompt()
                        .user(message)
                        .call()
                        .content();

            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == maxAttempts) {
                    throw new RuntimeException("请求频率超限，请稍后再试", e);
                }
                // 优先读响应头里的 Retry-After，没有就用默认等待时间
                String retryAfter = e.getResponseHeaders() != null
                        ? e.getResponseHeaders().getFirst("Retry-After") : null;
                long waitMs = retryAfter != null ? Long.parseLong(retryAfter) * 1000 : delayMs;
                log.warn("触发限流，等待 {}ms 后重试（第 {}/{} 次）", waitMs, attempt, maxAttempts);
                sleep(waitMs);

            } catch (HttpServerErrorException e) {
                if (attempt == maxAttempts) throw new RuntimeException("AI 服务异常", e);
                log.warn("服务端错误，{}ms 后重试（第 {}/{} 次）", delayMs, attempt, maxAttempts);
                sleep(delayMs);
                delayMs *= 2; // 指数退避

            } catch (Exception e) {
                throw new RuntimeException("AI 调用失败", e);
            }
        }
        return "AI 服务暂时不可用";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试被中断", e);
        }
    }
}