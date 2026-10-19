package com.jichi.prompt.service;

import com.jichi.prompt.entity.AbTestResult;
import com.jichi.prompt.entity.ExperimentReport;
import com.jichi.prompt.entity.VariantStats;
import com.jichi.prompt.repository.AbTestResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AbTestAnalysisService {

    private final AbTestResultRepository repository;

    public AbTestAnalysisService(AbTestResultRepository repository) {
        this.repository = repository;
    }

    public ExperimentReport analyze(String experimentId) {
        List<AbTestResult> results = repository.findByExperimentId(experimentId);

        List<AbTestResult> aResults = results.stream()
                .filter(r -> "A".equals(r.getVariant())).toList();
        List<AbTestResult> bResults = results.stream()
                .filter(r -> "B".equals(r.getVariant())).toList();

        VariantStats statsA = calculateStats("A", aResults);
        VariantStats statsB = calculateStats("B", bResults);

        // 简单判断：评分差超过 0.3 分且样本量足够（各≥50）认为有显著差异
        String winner = "INCONCLUSIVE";
        String conclusion = "样本量不足或差异不显著，建议继续收集数据";

        if (aResults.size() >= 50 && bResults.size() >= 50) {
            double ratingDiff = statsB.avgRating() - statsA.avgRating();
            if (ratingDiff > 0.3) {
                winner = "B";
                conclusion = String.format("B 版本表现更好，评分高出 %.2f 分", ratingDiff);
            } else if (ratingDiff < -0.3) {
                winner = "A";
                conclusion = String.format("A 版本表现更好，评分高出 %.2f 分", -ratingDiff);
            }
        }

        return new ExperimentReport(experimentId, statsA, statsB, winner, conclusion);
    }

    private VariantStats calculateStats(String variant, List<AbTestResult> results) {
        if (results.isEmpty()) {
            return new VariantStats(variant, 0, 0, 0, 0);
        }

        long total = results.size();
        double successRate = results.stream().filter(AbTestResult::isSuccess).count() * 1.0 / total;

        List<Integer> ratings = results.stream()
                .map(AbTestResult::getUserRating)
                .filter(Objects::nonNull)
                .toList();

        double avgRating = ratings.stream().mapToInt(i -> i).average().orElse(0);
        double stdDev = calculateStdDev(ratings, avgRating);

        return new VariantStats(variant, total, successRate, avgRating, stdDev);
    }

    private double calculateStdDev(List<Integer> values, double mean) {
        if (values.size() < 2) return 0;
        double sumSq = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum();
        return Math.sqrt(sumSq / (values.size() - 1));
    }
}