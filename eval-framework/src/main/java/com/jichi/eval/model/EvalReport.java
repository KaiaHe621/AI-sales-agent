package com.jichi.eval.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EvalReport {

    private String runId;
    private LocalDateTime runAt;
    private int totalCases;
    private int passedCases;
    private double passRate;
    private double avgScore;
    private List<EvalResult> results;

    public static EvalReport from(String runId, List<EvalResult> results) {
        int total = results.size();
        long passed = results.stream().filter(EvalResult::isPassed).count();
        double avgScore = results.stream()
                .mapToDouble(EvalResult::getScore)
                .average()
                .orElse(0.0);

        return EvalReport.builder()
                .runId(runId)
                .runAt(LocalDateTime.now())
                .totalCases(total)
                .passedCases((int) passed)
                .passRate(total == 0 ? 0.0 : (double) passed / total)
                .avgScore(avgScore)
                .results(results)
                .build();
    }
}