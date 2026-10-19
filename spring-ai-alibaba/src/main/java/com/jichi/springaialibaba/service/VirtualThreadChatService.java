package com.jichi.springaialibaba.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class VirtualThreadChatService {

    // 每个任务一个虚拟线程，资源消耗极低
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ChatClient deepseekChatClient;
    private final ChatClient qwenChatClient;

    public VirtualThreadChatService(
            @Qualifier("primaryChatClient") ChatClient deepseekChatClient,
            @Qualifier("backupChatClient") ChatClient qwenChatClient) {
        this.deepseekChatClient = deepseekChatClient;
        this.qwenChatClient = qwenChatClient;
    }

    /**
     * 赛马模式：同时向多个模型提问，任何一个返回就立刻响应，其余取消
     * 适合对延迟敏感、不要求特定模型的场景
     */
    public String fastestResponse(String question) throws Exception {
        List<Callable<String>> tasks = List.of(
                () -> deepseekChatClient.prompt().user(question).call().content(),
                () -> qwenChatClient.prompt().user(question).call().content()
        );
        // invokeAny：最快完成的那个返回，其他的自动取消
        return executor.invokeAny(tasks, 30, TimeUnit.SECONDS);
    }

    /**
     * 全量模式：所有模型都跑完，把结果一起返回
     */
    public Map<String, String> allResponses(String question) throws Exception {
        CompletableFuture<String> deepseekFuture = CompletableFuture.supplyAsync(
                () -> deepseekChatClient.prompt().user(question).call().content(), executor);
        CompletableFuture<String> qwenFuture = CompletableFuture.supplyAsync(
                () -> qwenChatClient.prompt().user(question).call().content(), executor);

        CompletableFuture.allOf(deepseekFuture, qwenFuture).get(30, TimeUnit.SECONDS);

        Map<String, String> results = new HashMap<>();
        results.put("deepseek", deepseekFuture.get());
        results.put("qwen", qwenFuture.get());
        return results;
    }
}