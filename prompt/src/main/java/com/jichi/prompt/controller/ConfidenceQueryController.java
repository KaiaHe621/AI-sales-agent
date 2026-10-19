package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.jichi.prompt.entity.AnswerWithConfidence;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/confidence")
public class ConfidenceQueryController {

    private final DashScopeChatModel chatModel;

    public ConfidenceQueryController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/query")
    public AnswerWithConfidence queryWithConfidence(
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int samples) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withTemperature(0.7)
                .build();

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> chatModel.call(new Prompt(
                            new UserMessage(question + "\n只输出最终答案，不要解释。"),
                            options
                    )).getResult().getOutput().getText().trim(),
                    executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);

        List<String> answers = futures.stream()
                .map(f -> { try { return f.get(); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull)
                .toList();

        Map<String, Long> freq = answers.stream()
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));

        Map.Entry<String, Long> top = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        double confidence = (double) top.getValue() / answers.size();

        return new AnswerWithConfidence(top.getKey(), confidence, samples);
    }
}