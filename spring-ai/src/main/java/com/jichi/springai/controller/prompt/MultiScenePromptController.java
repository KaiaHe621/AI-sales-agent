package com.jichi.springai.controller.prompt;

import com.jichi.springai.service.PromptDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/multi-scene")
public class MultiScenePromptController {

    private final PromptDemoService promptDemoService;

    public MultiScenePromptController(PromptDemoService promptDemoService) {
        this.promptDemoService = promptDemoService;
    }

    @GetMapping("/review")
    public String review(
            @RequestParam(defaultValue = "Java") String language,
            @RequestParam String code
    ) {
        return promptDemoService.codeReview(language, code);
    }

    @GetMapping("/translate")
    public String translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "英文") String targetLanguage
    ) {
        return promptDemoService.translate(text, targetLanguage);
    }

    @GetMapping("/qa")
    public String qa(
            @RequestParam String domain,
            @RequestParam String question
    ) {
        return promptDemoService.domainQA(domain, question);
    }
}