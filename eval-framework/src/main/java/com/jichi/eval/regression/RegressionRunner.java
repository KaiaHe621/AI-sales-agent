package com.jichi.eval.regression;

import com.jichi.eval.model.EvalReport;
import com.jichi.eval.model.EvalResult;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegressionRunner {

    /**
     * 分数下降超过此阈值视为 Regression
     */
    private static final double REGRESSION_THRESHOLD = 0.1;

    /**
     * 对比两份评估报告，返回 Regression 分析结果
     */
    public RegressionReport compare(EvalReport baseline, EvalReport current) {
        Map<String, Double> baselineScores = toScoreMap(baseline);
        Map<String, Double> currentScores = toScoreMap(current);

        List<CaseDiff> regressions = new ArrayList<>();
        List<CaseDiff> improvements = new ArrayList<>();
        List<CaseDiff> stable = new ArrayList<>();

        for (String caseId : currentScores.keySet()) {
            double currentScore = currentScores.get(caseId);
            Double baselineScore = baselineScores.get(caseId);

            if (baselineScore == null) {
                // 新增用例，跳过对比
                continue;
            }

            double delta = currentScore - baselineScore;
            CaseDiff diff = CaseDiff.builder()
                    .caseId(caseId)
                    .baselineScore(baselineScore)
                    .currentScore(currentScore)
                    .delta(delta)
                    .build();

            if (delta < -REGRESSION_THRESHOLD) {
                regressions.add(diff);
            } else if (delta > REGRESSION_THRESHOLD) {
                improvements.add(diff);
            } else {
                stable.add(diff);
            }
        }

        double overallDelta = current.getAvgScore() - baseline.getAvgScore();
        boolean hasRegression = !regressions.isEmpty() || overallDelta < -REGRESSION_THRESHOLD;

        RegressionReport report = RegressionReport.builder()
                .baselineRunId(baseline.getRunId())
                .currentRunId(current.getRunId())
                .baselineAvgScore(baseline.getAvgScore())
                .currentAvgScore(current.getAvgScore())
                .overallDelta(overallDelta)
                .hasRegression(hasRegression)
                .regressions(regressions)
                .improvements(improvements)
                .stable(stable)
                .build();

        logSummary(report);
        return report;
    }

    private Map<String, Double> toScoreMap(EvalReport report) {
        return report.getResults().stream()
                .collect(Collectors.toMap(
                        EvalResult::getCaseId,
                        EvalResult::getScore,
                        (a, b) -> (a + b) / 2.0  // 同一用例多条结果取均值
                ));
    }

    private void logSummary(RegressionReport report) {
        log.info("=== Regression 对比报告 ===");
        log.info("Baseline: {}（均分 {}）", report.getBaselineRunId(),
                String.format("%.3f", report.getBaselineAvgScore()));
        log.info("当前版本: {}（均分 {}）", report.getCurrentRunId(),
                String.format("%.3f", report.getCurrentAvgScore()));
        log.info("整体变化: {}", String.format("%+.3f", report.getOverallDelta()));
        log.info("退化用例: {} 条 | 改善用例: {} 条 | 稳定用例: {} 条",
                report.getRegressions().size(),
                report.getImprovements().size(),
                report.getStable().size());

        if (report.isHasRegression()) {
            log.warn("检测到 Regression！退化用例：");
            report.getRegressions().forEach(d ->
                    log.warn("  [{}] {} → {}（下降 {}）",
                            d.getCaseId(),
                            String.format("%.3f", d.getBaselineScore()),
                            String.format("%.3f", d.getCurrentScore()),
                            String.format("%.3f", d.getDelta())));
        }
    }

    @Data
    @Builder
    public static class RegressionReport {
        private String baselineRunId;
        private String currentRunId;
        private double baselineAvgScore;
        private double currentAvgScore;
        private double overallDelta;
        private boolean hasRegression;
        private List<CaseDiff> regressions;
        private List<CaseDiff> improvements;
        private List<CaseDiff> stable;
    }

    @Data
    @Builder
    public static class CaseDiff {
        private String caseId;
        private double baselineScore;
        private double currentScore;
        private double delta;
    }
}