package com.jichi.springai.controller.prompt;

import com.jichi.springai.service.CodeReviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/template")
public class TemplateCodeReviewController {

    private final CodeReviewService codeReviewService;

    public TemplateCodeReviewController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/review")
    public String review(
            @RequestParam(defaultValue = "Java") String language,
            @RequestParam String code
    ) {
        return codeReviewService.review(language, code);
    }
}