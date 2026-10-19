package com.jichi.springaialibaba.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BadApproachService {

    private final ChatClient deepseekChatClient;
    private final ChatClient qwenChatClient;

    public BadApproachService(
            @Qualifier("primaryChatClient") ChatClient deepseekChatClient,
            @Qualifier("backupChatClient") ChatClient qwenChatClient) {
        this.deepseekChatClient = deepseekChatClient;
        this.qwenChatClient = qwenChatClient;
    }

    // 串行调用：总耗时 = A耗时 + B耗时，两个请求完全没有依赖，却白白等了一倍
    public Map<String, String> serialChat(String question) {
        String deepseekAnswer = deepseekChatClient.prompt()
                .user(question).call().content();  // 假设耗时 3s

        String qwenAnswer = qwenChatClient.prompt()
                .user(question).call().content();  // 假设耗时 2s

        return Map.of("deepseek", deepseekAnswer, "qwen", qwenAnswer);
        // 总耗时：5s，实际完全可以压缩到 3s
    }
}