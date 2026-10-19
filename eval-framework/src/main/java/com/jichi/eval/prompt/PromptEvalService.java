package com.jichi.eval.prompt;

import com.jichi.eval.dataset.GoldenDataset;
import com.jichi.eval.evaluator.Evaluator;
import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalReport;
import com.jichi.eval.model.EvalResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class PromptEvalService {

    private final GoldenDataset goldenDataset;
    private final Map<String, Evaluator> evaluators;
    private final PromptRegistry promptRegistry;
    private final ChatClient chatClient;

    public PromptEvalService(GoldenDataset goldenDataset,
                              Map<String, Evaluator> evaluators,
                              PromptRegistry promptRegistry,
                              ChatClient.Builder chatClientBuilder) {
        this.goldenDataset = goldenDataset;
        this.evaluators = evaluators;
        this.promptRegistry = promptRegistry;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 对两个 Prompt 版本做 A/B 对比评估
     *
     * @param versionA     对照组版本号
     * @param versionB     实验组版本号
     * @param evaluatorKey 使用的评估器
     */
    public PromptABReport compare(String versionA, String versionB,
                                   String evaluatorKey) throws IOException {
        PromptVersion promptA = promptRegistry.get(versionA)
                .orElseThrow(() -> new IllegalArgumentException("找不到 Prompt 版本：" + versionA));
        PromptVersion promptB = promptRegistry.get(versionB)
                .orElseThrow(() -> new IllegalArgumentException("找不到 Prompt 版本：" + versionB));

        Evaluator evaluator = evaluators.get(evaluatorKey);
        if (evaluator == null) {
            throw new IllegalArgumentException("找不到评估器：" + evaluatorKey);
        }

        List<EvalCase> cases = goldenDataset.load();

        EvalReport reportA = runWithPrompt(promptA, cases, evaluator);
        EvalReport reportB = runWithPrompt(promptB, cases, evaluator);

        return buildABReport(versionA, versionB, reportA, reportB);
    }

    private EvalReport runWithPrompt(PromptVersion prompt,
                                      List<EvalCase> cases,
                                      Evaluator evaluator) {
        List<EvalResult> results = new ArrayList<>();

        for (EvalCase evalCase : cases) {
            try {
                // 用指定的 System Prompt 调用模型
                String response = chatClient.prompt()
                        .system(prompt.getSystemPrompt())
                        .user(evalCase.getInput())
                        .call()
                        .content();

                EvalResult result = evaluator.evaluate(evalCase, response);
                results.add(result);

            } catch (Exception e) {
                log.error("[{}][{}] 调用失败：{}", prompt.getVersion(), evalCase.getId(), e.getMessage());
                results.add(EvalResult.builder()
                        .caseId(evalCase.getId())
                        .actualOutput("")
                        .score(0.0)
                        .dimension(evaluatorKey(evaluator))
                        .reason("调用失败：" + e.getMessage())
                        .passed(false)
                        .build());
            }
        }

        String runId = prompt.getVersion() + "-" + UUID.randomUUID().toString().substring(0, 6);
        return EvalReport.from(runId, results);
    }

    private PromptABReport buildABReport(String versionA, String versionB,
                                          EvalReport reportA, EvalReport reportB) {
        double delta = reportB.getAvgScore() - reportA.getAvgScore();
        double improvement = reportA.getAvgScore() == 0 ? 0 :
                delta / reportA.getAvgScore() * 100;

        String recommendation;
        if (delta > 0.05) {
            recommendation = "推荐使用 " + versionB + "（平均分提升 " + String.format("%.1f%%", improvement) + "）";
        } else if (delta < -0.05) {
            recommendation = "保留 " + versionA + "（" + versionB + " 性能下降 " + String.format("%.1f%%", Math.abs(improvement)) + "）";
        } else {
            recommendation = "两个版本性能相近，建议考虑其他因素（如可读性、简洁度）";
        }

        return PromptABReport.builder()
                .versionA(versionA)
                .versionB(versionB)
                .scoreA(reportA.getAvgScore())
                .scoreB(reportB.getAvgScore())
                .delta(delta)
                .improvementPercent(improvement)
                .recommendation(recommendation)
                .reportA(reportA)
                .reportB(reportB)
                .build();
    }

    private String evaluatorKey(Evaluator evaluator) {
        return evaluator.getClass().getSimpleName();
    }

    @Data
    @Builder
    public static class PromptABReport {
        private String versionA;
        private String versionB;
        private double scoreA;
        private double scoreB;
        private double delta;
        private double improvementPercent;
        private String recommendation;
        private EvalReport reportA;
        private EvalReport reportB;
    }
}