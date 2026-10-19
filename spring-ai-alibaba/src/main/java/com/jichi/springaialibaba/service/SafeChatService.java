package com.jichi.springaialibaba.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class SafeChatService {

    private static final Logger log = LoggerFactory.getLogger(SafeChatService.class);

    private final ChatClient chatClient;

    public SafeChatService(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String safeChat(String message) {
        try {
            return chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429 限流，告诉用户稍后再试
            throw new RuntimeException("服务繁忙，请稍后再试", e);

        } catch (HttpClientErrorException.Unauthorized e) {
            // 401 认证失败，不要把具体原因暴露给用户
            log.error("AI 服务认证失败，请检查 API Key", e);
            throw new RuntimeException("AI 服务暂时不可用");

        } catch (HttpServerErrorException e) {
            // 5xx 服务端错误
            log.error("AI 服务端错误: {}", e.getMessage());
            throw new RuntimeException("AI 服务暂时不可用，请稍后重试");

        } catch (Exception e) {
            log.error("AI 调用未知错误", e);
            throw new RuntimeException("AI 服务异常");
        }
    }
}