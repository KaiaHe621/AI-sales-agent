package com.jichi.springaialibaba.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class BatchAnalysisService {

    private final ChatClient chatClient;

    public BatchAnalysisService(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public enum Sentiment {POSITIVE, NEGATIVE, NEUTRAL}

    public record AnalysisResult(String text, Sentiment sentiment, int score) {
    }

    // 提取为独立 record，避免放在方法体内导致编译问题
    public record SentimentResult(Sentiment sentiment, int score) {
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BatchAnalysisService.class);

    /**
     * 并行批量情感分析
     *
     * @param texts       要分析的文本列表
     * @param concurrency 并发数，建议 5-10，太高容易触发模型 API 限流
     */
    public List<AnalysisResult> batchAnalyze(List<String> texts, int concurrency) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // Semaphore 控制同时运行的任务数，permits 用完后新任务阻塞等待
        Semaphore semaphore = new Semaphore(concurrency);

        List<CompletableFuture<AnalysisResult>> futures = texts.stream()
                .map(text -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire(); // 拿到令牌才能继续，没令牌就等
                        try {
                            // system prompt 必须明确告诉模型返回 JSON，否则 .entity() 解析失败返回 null
                            SentimentResult result = chatClient.prompt()
                                    .system("""
                                            分析文本情感，严格按以下 JSON 格式返回，不要有其他内容：
                                            {"sentiment":"POSITIVE","score":8}
                                            sentiment 只能是 POSITIVE / NEGATIVE / NEUTRAL，score 是 1-10 的整数，10 最正面。
                                            """)
                                    .user(text)
                                    .call()
                                    .entity(SentimentResult.class);
                            return new AnalysisResult(text, result.sentiment(), result.score());
                        } finally {
                            semaphore.release(); // 无论成功失败都要释放令牌
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toList();

        // 收集所有结果，单条失败记日志但不影响整体
        return futures.stream()
                .map(f -> {
                    try {
                        return f.get(60, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("批量分析单条失败：{}", e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();
    }
}