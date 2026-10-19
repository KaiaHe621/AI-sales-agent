package com.jichi.eval.agent;

import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalResult;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTraceEvaluator {

    /**
     * 评估一条 Agent Trace
     *
     * @param evalCase       测试用例（含期望工具调用序列）
     * @param trace          实际执行的 Trace
     * @param expectedTools  期望调用的工具列表（按顺序）
     */
    public TraceEvalResult evaluate(EvalCase evalCase,
                                    AgentTrace trace,
                                    List<String> expectedTools) {
        double toolAccuracy = evaluateToolAccuracy(trace, expectedTools);
        double efficiency = evaluateEfficiency(trace, expectedTools.size());
        double answerQuality = evaluateAnswerQuality(evalCase, trace);

        double overallScore = toolAccuracy * 0.4 + efficiency * 0.3 + answerQuality * 0.3;

        return TraceEvalResult.builder()
                .traceId(trace.getTraceId())
                .caseId(evalCase.getId())
                .toolAccuracyScore(toolAccuracy)
                .efficiencyScore(efficiency)
                .answerQualityScore(answerQuality)
                .overallScore(overallScore)
                .actualToolCalls(getToolNames(trace))
                .expectedToolCalls(expectedTools)
                .build();
    }

    /**
     * 工具调用正确性：有没有调该调的，没有调不该调的
     */
    private double evaluateToolAccuracy(AgentTrace trace, List<String> expectedTools) {
        List<String> actualTools = getToolNames(trace);

        if (expectedTools.isEmpty()) {
            return actualTools.isEmpty() ? 1.0 : 0.5;
        }

        // 命中：期望工具被实际调用了
        long hit = expectedTools.stream()
                .filter(actualTools::contains)
                .count();

        // 误调：实际调了但期望里没有
        long extraCalls = actualTools.stream()
                .filter(t -> !expectedTools.contains(t))
                .count();

        double recall = (double) hit / expectedTools.size();
        double precision = actualTools.isEmpty() ? 1.0 :
                (double) (actualTools.size() - extraCalls) / actualTools.size();

        // F1 分数
        if (precision + recall == 0) return 0.0;
        return 2 * precision * recall / (precision + recall);
    }

    /**
     * 效率：实际工具调用次数 vs 期望次数，越接近越好
     */
    private double evaluateEfficiency(AgentTrace trace, int expectedCallCount) {
        int actualCallCount = trace.getToolCallCount();

        if (expectedCallCount == 0) {
            return actualCallCount == 0 ? 1.0 : Math.max(0, 1.0 - actualCallCount * 0.2);
        }

        double ratio = (double) actualCallCount / expectedCallCount;
        if (ratio <= 1.0) {
            return ratio;  // 少调或恰好，按比例得分
        } else {
            return Math.max(0, 2.0 - ratio);  // 多调，线性惩罚
        }
    }

    /**
     * 最终答案质量：是否包含期望输出的关键词
     */
    private double evaluateAnswerQuality(EvalCase evalCase, AgentTrace trace) {
        String answer = trace.getFinalAnswer();
        String expected = evalCase.getExpectedOutput();

        if (answer == null || answer.isBlank()) return 0.0;
        if (expected == null || expected.isBlank()) return 1.0;

        String[] fragments = expected.split("[，。、；：,.;:]");
        long total = 0;
        long matched = 0;
        for (String fragment : fragments) {
            String kw = fragment.trim();
            if (kw.length() >= 2) {
                total++;
                if (answer.contains(kw)) matched++;
            }
        }
        return total == 0 ? 1.0 : (double) matched / total;
    }

    private List<String> getToolNames(AgentTrace trace) {
        return trace.getSteps().stream()
                .filter(s -> s.getType() == AgentTrace.TraceStep.StepType.ACTION)
                .map(AgentTrace.TraceStep::getToolName)
                .toList();
    }

    @Data
    @Builder
    public static class TraceEvalResult {
        private String traceId;
        private String caseId;
        private double toolAccuracyScore;
        private double efficiencyScore;
        private double answerQualityScore;
        private double overallScore;
        private List<String> actualToolCalls;
        private List<String> expectedToolCalls;

        public boolean isPassed() {
            return overallScore >= 0.6;
        }
    }
}