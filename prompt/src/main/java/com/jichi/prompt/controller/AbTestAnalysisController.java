package com.jichi.prompt.controller;

import com.jichi.prompt.entity.ExperimentReport;
import com.jichi.prompt.service.AbTestAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ab-test")
public class AbTestAnalysisController {

    private final AbTestAnalysisService analysisService;

    public AbTestAnalysisController(AbTestAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/report/{experimentId}")
    public ExperimentReport getReport(@PathVariable String experimentId) {
        return analysisService.analyze(experimentId);
    }
}