package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.jichi.prompt.entity.ContractRisk;
import com.jichi.prompt.enums.Verdict;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ContractAnalysisService {

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<ContractRisk> converter;

    public ContractAnalysisService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(ContractRisk.class);
    }

    public ContractRisk analyzeWithConsistency(String clause) throws Exception {
        int sampleCount = 5;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withTemperature(0.5)
                .build();

        String userContent = "分析这个合同条款：\n" + clause
                + "\n\n请先逐步分析，再给出结论。\n\n" + converter.getFormat();

        List<CompletableFuture<ContractRisk>> futures = new ArrayList<>();
        for (int i = 0; i < sampleCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                String raw = chatModel.call(new Prompt(
                        List.of(
                                new SystemMessage("你是合同法律顾问，分析合同条款是否存在法律风险。先思考，再给出结论。"),
                                new UserMessage(userContent)
                        ),
                        options
                )).getResult().getOutput().getText();
                return converter.convert(raw);
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);

        List<ContractRisk> results = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return aggregateResults(results);
    }

    private ContractRisk aggregateResults(List<ContractRisk> results) {
        long yesCount = results.stream().filter(r -> r.hasRisk() == Verdict.YES).count();
        long noCount = results.stream().filter(r -> r.hasRisk() == Verdict.NO).count();

        Verdict majorityVerdict = yesCount >= noCount ? Verdict.YES : Verdict.NO;

        double avgSeverity = results.stream()
                .filter(r -> r.hasRisk() == majorityVerdict)
                .mapToInt(ContractRisk::severity)
                .average()
                .orElse(5.0);

        String topRiskType = results.stream()
                .filter(r -> r.hasRisk() == majorityVerdict)
                .map(ContractRisk::riskType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知风险");

        return new ContractRisk(majorityVerdict, topRiskType, (int) Math.round(avgSeverity));
    }
}