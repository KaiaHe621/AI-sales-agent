package com.jichi.springaialibaba.controller.parallel;

import com.jichi.springaialibaba.service.BatchAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
public class BatchAnalysisController {

    private final BatchAnalysisService batchAnalysisService;

    public BatchAnalysisController(BatchAnalysisService batchAnalysisService) {
        this.batchAnalysisService = batchAnalysisService;
    }

    @PostMapping("/analyze")
    public List<BatchAnalysisService.AnalysisResult> analyze(
            @RequestBody List<String> texts,
            @RequestParam(defaultValue = "5") int concurrency) {
        return batchAnalysisService.batchAnalyze(texts, concurrency);
    }
}