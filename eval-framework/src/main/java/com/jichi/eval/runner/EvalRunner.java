package com.jichi.eval.runner;

import com.jichi.eval.dataset.GoldenDataset;
import com.jichi.eval.evaluator.Evaluator;
import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalReport;
import com.jichi.eval.model.EvalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvalRunner {

    private final GoldenDataset goldenDataset;
    private final Map<String, Evaluator> evaluators;  // Spring 自动注入所有 Evaluator Bean
    private final RestTemplate restTemplate;

    /**
     * 对外部 AI 服务接口跑评估
     *
     * @param aiEndpoint   被测 AI 系统的接口地址，如 http://localhost:8080/ai/chat
     * @param evaluatorKey 使用哪个评估器（keywordEvaluator / lengthEvaluator）
     */
    public EvalReport run(String aiEndpoint, String evaluatorKey) throws IOException {
        Evaluator evaluator = evaluators.get(evaluatorKey);
        if (evaluator == null) {
            throw new IllegalArgumentException("找不到评估器：" + evaluatorKey
                    + "，可选：" + evaluators.keySet());
        }

        List<EvalCase> cases = goldenDataset.load();
        List<EvalResult> results = new ArrayList<>();

        for (EvalCase evalCase : cases) {
            try {
                // 调用被测 AI 系统
                String actualOutput = callAiSystem(aiEndpoint, evalCase.getInput());
                log.info("[{}] 输入：{}", evalCase.getId(), evalCase.getInput());
                log.info("[{}] 输出：{}", evalCase.getId(), actualOutput);

                // 评估
                EvalResult result = evaluator.evaluate(evalCase, actualOutput);
                results.add(result);

                log.info("[{}] 评分：{} | 通过：{} | 原因：{}",
                        evalCase.getId(), result.getScore(), result.isPassed(), result.getReason());

            } catch (Exception e) {
                log.error("[{}] 调用 AI 系统失败：{}", evalCase.getId(), e.getMessage());
                results.add(EvalResult.builder()
                        .caseId(evalCase.getId())
                        .actualOutput("")
                        .score(0.0)
                        .dimension(evaluatorKey)
                        .reason("调用失败：" + e.getMessage())
                        .passed(false)
                        .build());
            }
        }

        String runId = UUID.randomUUID().toString().substring(0, 8);
        EvalReport report = EvalReport.from(runId, results);

        log.info("评估完成 [{}]：通过率 {:.1%}，平均分 {:.3f}",
                runId, report.getPassRate(), report.getAvgScore());

        return report;
    }

    /**
     * 调用被测 AI 系统，返回文本输出
     * 假设 AI 系统接受 POST JSON {"message": "..."} 返回 {"reply": "..."}
     */
    private String callAiSystem(String endpoint, String input) {
        var requestBody = Map.of("message", input);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                endpoint, requestBody, Map.class);
        if (response == null) return "";
        return String.valueOf(response.getOrDefault("reply", ""));
    }
}