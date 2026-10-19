package com.jichi.prompt.controller;

import com.jichi.prompt.service.TechDecisionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tech-eval")
public class TechDecisionController {

    private final TechDecisionService techDecisionService;

    public TechDecisionController(TechDecisionService techDecisionService) {
        this.techDecisionService = techDecisionService;
    }

    @PostMapping
    public TechDecisionService.TechEvaluation evaluate(@RequestBody EvalRequest request) {
        return techDecisionService.evaluate(request.proposal(), request.context());
    }

    record EvalRequest(String proposal, String context) {}
}