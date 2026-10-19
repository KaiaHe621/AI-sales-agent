package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class BulkExtractionService {

    private final DashScopeChatModel chatModel;
    private final ExecutorService executor =
            Executors.newVirtualThreadPerTaskExecutor();

    // 控制并发数，避免触发 API 限流
    private final Semaphore semaphore = new Semaphore(5);

    public BulkExtractionService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public <T> List<T> extractBatch(List<String> texts, Class<T> targetClass,
                                     String extractionPrompt) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(targetClass);

        List<CompletableFuture<T>> futures = texts.stream()
                .map(text -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            String raw = chatModel.call(new Prompt(
                                    new UserMessage(extractionPrompt + "\n\n"
                                            + converter.getFormat() + "\n\n" + text)
                            )).getResult().getOutput().getText();
                            return converter.convert(raw);
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}