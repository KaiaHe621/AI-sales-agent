package com.jichi.springaialibaba.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MultiModelVotingService {

    private final List<ChatClient> clients;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MultiModelVotingService(
            @Qualifier("primaryChatClient") ChatClient c1,
            @Qualifier("backupChatClient") ChatClient c2) {
        this.clients = List.of(c1, c2);
    }

    public record VoteResult(String answer, int votes, double confidence) {
    }

    /**
     * 多模型投票，返回票数最多的答案和置信度
     */
    public VoteResult vote(String question) throws Exception {
        List<CompletableFuture<String>> futures = clients.stream()
                .map(client -> CompletableFuture.supplyAsync(
                        () -> client.prompt()
                                .system("只回答 YES 或 NO，不要有其他内容")
                                .user(question)
                                .call()
                                .content()
                                .trim()
                                .toUpperCase(),
                        executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);

        // 统计各答案的票数
        Map<String, Long> votes = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return "ERROR";
                    }
                })
                .filter(r -> !r.equals("ERROR"))
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        String winner = votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");

        long winnerVotes = votes.getOrDefault(winner, 0L);
        double confidence = (double) winnerVotes / clients.size();

        return new VoteResult(winner, (int) winnerVotes, confidence);
    }
}