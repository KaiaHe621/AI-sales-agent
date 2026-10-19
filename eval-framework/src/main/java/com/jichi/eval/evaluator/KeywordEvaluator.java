package com.jichi.eval.evaluator;

import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalResult;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("keywordEvaluator")
public class KeywordEvaluator implements Evaluator {

    @Override
    public EvalResult evaluate(EvalCase evalCase, String actualOutput) {
        if (actualOutput == null || actualOutput.isBlank()) {
            return EvalResult.builder()
                    .caseId(evalCase.getId())
                    .actualOutput("")
                    .score(0.0)
                    .dimension("keyword")
                    .reason("输出为空")
                    .passed(false)
                    .build();
        }

        List<String> keywords = resolveKeywords(evalCase);

        if (keywords.isEmpty()) {
            return buildResult(evalCase, actualOutput, 1.0, "无关键词约束，默认通过");
        }

        // 对实际输出做标准化：去掉 Markdown 的反引号等符号，避免 `@Annotation` 匹配不上 @Annotation
        String normalizedOutput = normalizeText(actualOutput);

        long matched = keywords.stream()
                .filter(keyword -> normalizedOutput.contains(normalizeText(keyword)))
                .count();

        double score = (double) matched / keywords.size();
        String reason = String.format("关键词命中 %d/%d：%s", matched, keywords.size(), keywords);

        return buildResult(evalCase, actualOutput, score, reason);
    }

    /**
     * 优先使用 keywords 字段（显式关键词列表）。
     * 如果没有配置，则回退到从 expectedOutput 按标点拆分——适合极简 golden.json 场景，
     * 但注意这种方式对长句的命中率较低。
     */
    private List<String> resolveKeywords(EvalCase evalCase) {
        if (evalCase.getKeywords() != null && !evalCase.getKeywords().isEmpty()) {
            return evalCase.getKeywords().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        if (evalCase.getExpectedOutput() == null || evalCase.getExpectedOutput().isBlank()) {
            return List.of();
        }

        // 回退策略：把期望输出按标点拆成片段，过滤掉太短的
        String[] fragments = evalCase.getExpectedOutput().split("[，。、；：,.;:]");
        return Arrays.stream(fragments)
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .toList();
    }

    /** 去掉 Markdown 符号（反引号、星号等），用于字符串匹配前的归一化 */
    private String normalizeText(String text) {
        return text.replace("`", "").replace("*", "").replace("**", "").trim();
    }

    private EvalResult buildResult(EvalCase evalCase, String actualOutput,
                                   double score, String reason) {
        return EvalResult.builder()
                .caseId(evalCase.getId())
                .actualOutput(actualOutput)
                .score(score)
                .dimension("keyword")
                .reason(reason)
                .passed(score >= passThreshold())
                .build();
    }
}