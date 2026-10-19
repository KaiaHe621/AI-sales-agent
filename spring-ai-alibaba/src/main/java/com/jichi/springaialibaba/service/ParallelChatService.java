package com.jichi.springaialibaba.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ParallelChatService {

    private final ChatClient deepseekChatClient;
    private final ChatClient qwenChatClient;

    public ParallelChatService(
            @Qualifier("primaryChatClient") ChatClient deepseekChatClient,
            @Qualifier("backupChatClient") ChatClient qwenChatClient) {
        this.deepseekChatClient = deepseekChatClient;
        this.qwenChatClient = qwenChatClient;
    }

    /**
     * 并行调用两个模型，总耗时 ≈ max(A耗时, B耗时)
     */
    public Map<String, String> parallelChat(String question) throws Exception {
        // 两个请求同时发出，各自在独立线程里跑
        CompletableFuture<String> deepseekFuture = CompletableFuture.supplyAsync(
                () -> deepseekChatClient.prompt().user(question).call().content()
        );
        CompletableFuture<String> qwenFuture = CompletableFuture.supplyAsync(
                () -> qwenChatClient.prompt().user(question).call().content()
        );

        // allOf 等两个都完成，超时 30s 抛异常
        CompletableFuture.allOf(deepseekFuture, qwenFuture).get(30, TimeUnit.SECONDS);

        return Map.of(
                "deepseek", deepseekFuture.get(),
                "qwen", qwenFuture.get()
        );
        // 总耗时：≈ 3s（取两个里最慢的）
    }
}