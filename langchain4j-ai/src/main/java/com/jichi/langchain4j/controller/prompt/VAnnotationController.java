package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.model.CodeReviewRequest;
import com.jichi.langchain4j.service.CodeReviewer;
import com.jichi.langchain4j.service.ReportGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt/v")
public class VAnnotationController {

    private final ReportGenerator reportGenerator;
    private final CodeReviewer codeReviewer;

    public VAnnotationController(ReportGenerator reportGenerator,
                                 CodeReviewer codeReviewer) {
        this.reportGenerator = reportGenerator;
        this.codeReviewer = codeReviewer;
    }

    @GetMapping("/report")
    public String generateReport(@RequestParam String type,
                                 @RequestParam String start,
                                 @RequestParam String end,
                                 @RequestParam String data,
                                 @RequestParam(defaultValue = "300") int wordCount,
                                 @RequestParam String focus) {
        return reportGenerator.generateReport(type, start, end, data, wordCount, focus);
    }

    @PostMapping("/review")
    public String reviewCode(@RequestBody CodeReviewRequest request) {
        return codeReviewer.review(request.language(), request.code(), request.focusAreas());
    }
}