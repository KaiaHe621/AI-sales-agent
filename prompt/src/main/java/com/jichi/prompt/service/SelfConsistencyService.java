package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class SelfConsistencyService {

    private final DashScopeChatModel chatModel;

    public SelfConsistencyService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Self-Consistency：采样 N 次，多数投票
     *
     * @param question    问题
     * @param sampleCount 采样次数（建议 3-7 次）
     * @return 出现最多的答案
     */
    public String query(String question, int sampleCount) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // Self-Consistency 需要 temperature > 0，否则每次输出相同，没有意义
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withTemperature(0.7)
                .build();

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < sampleCount; i++) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> chatModel.call(new Prompt(
                            new UserMessage(question + "\n\n让我们一步一步思考。"),
                            options
                    )).getResult().getOutput().getText(),
                    executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);

        List<String> answers = futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();

        return majorityVote(answers);
    }

    private String majorityVote(List<String> answers) {
        return answers.stream()
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(answers.get(0));
    }
}