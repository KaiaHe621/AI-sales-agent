package com.jichi.prompt.controller;

import com.jichi.prompt.service.CodeReviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/code-review")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;

    public CodeReviewController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/review")
    public String review(@RequestBody String code,
                         @RequestParam(defaultValue = "Java") String language) {
        return codeReviewService.review(code, language);
    }
}