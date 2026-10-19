package com.jichi.prompt.controller;

import com.jichi.prompt.entity.EvaluationResult;
import com.jichi.prompt.entity.QueryResponse;
import com.jichi.prompt.service.PromptEvaluator;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prompt-eval")
public class PromptEvaluatorController {

    private final PromptEvaluator promptEvaluator;

    public PromptEvaluatorController(PromptEvaluator promptEvaluator) {
        this.promptEvaluator = promptEvaluator;
    }

    record EvalRequest(String userQuery, String aiResponse) {}

    @PostMapping("/evaluate")
    public EvaluationResult evaluate(@RequestBody EvalRequest req) {
        return promptEvaluator.evaluate(req.userQuery(), req.aiResponse());
    }

    @PostMapping("/batch-evaluate")
    public Map<String, Double> batchEvaluate(@RequestBody List<QueryResponse> samples) {
        return promptEvaluator.batchEvaluate(samples);
    }
}