package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OfflineAbEvaluator {

    private final DashScopeChatModel chatModel;

    public OfflineAbEvaluator(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 离线对比两个 Prompt 在测试集上的表现
     */
    public OfflineComparisonReport compare(
            String promptA, String promptB,
            List<TestCase> goldTestCases) {

        List<CaseResult> results = goldTestCases.stream().map(tc -> {
            // 用 Prompt A 跑
            String responseA = chatModel.call(new Prompt(
                    List.of(new SystemMessage(promptA), new UserMessage(tc.input()))
            )).getResult().getOutput().getText();

            // 用 Prompt B 跑
            String responseB = chatModel.call(new Prompt(
                    List.of(new SystemMessage(promptB), new UserMessage(tc.input()))
            )).getResult().getOutput().getText();

            int scoreA = scoreResponse(tc, responseA);
            int scoreB = scoreResponse(tc, responseB);

            return new CaseResult(tc.id(), tc.input(), responseA, responseB, scoreA, scoreB);
        }).toList();

        double avgA = results.stream().mapToInt(CaseResult::scoreA).average().orElse(0);
        double avgB = results.stream().mapToInt(CaseResult::scoreB).average().orElse(0);
        String winner = avgB > avgA + 0.3 ? "B" : (avgA > avgB + 0.3 ? "A" : "INCONCLUSIVE");

        return new OfflineComparisonReport(results, avgA, avgB, winner);
    }

    private int scoreResponse(TestCase tc, String response) {
        String scoreText = chatModel.call(new Prompt(
                List.of(
                        new SystemMessage("你是质量评估专家，对 AI 回复打分（1-5）：1=差 3=一般 5=优秀"),
                        new UserMessage(String.format(
                                "问题：%s\n期望方向：%s\nAI回复：%s\n\n只输出分数（1-5的整数）",
                                tc.input(), tc.expectedDirection(), response))
                )
        )).getResult().getOutput().getText().trim();

        return Integer.parseInt(scoreText.replaceAll("[^1-5]", "5"));
    }

    public record TestCase(String id, String input, String expectedDirection) {}
    public record CaseResult(String id, String input, String responseA, String responseB,
                      int scoreA, int scoreB) {}
    public record OfflineComparisonReport(List<CaseResult> cases, double avgScoreA,
                                    double avgScoreB, String winner) {}
}