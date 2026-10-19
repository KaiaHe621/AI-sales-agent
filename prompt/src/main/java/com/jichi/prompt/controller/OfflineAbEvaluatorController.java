package com.jichi.prompt.controller;

import com.jichi.prompt.service.OfflineAbEvaluator;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ab-test")
public class OfflineAbEvaluatorController {

    private final OfflineAbEvaluator offlineAbEvaluator;

    public OfflineAbEvaluatorController(OfflineAbEvaluator offlineAbEvaluator) {
        this.offlineAbEvaluator = offlineAbEvaluator;
    }

    record OfflineEvalRequest(
        String promptA,
        String promptB,
        List<OfflineAbEvaluator.TestCase> testCases
    ) {}

    @PostMapping("/offline-eval")
    public OfflineAbEvaluator.OfflineComparisonReport evaluate(
            @RequestBody OfflineEvalRequest req) {
        return offlineAbEvaluator.compare(req.promptA(), req.promptB(), req.testCases());
    }
}