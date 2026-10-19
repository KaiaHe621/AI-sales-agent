package com.jichi.springai.controller.prompt;

import com.jichi.springai.service.TranslateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/translate")
public class TranslateController {

    private final TranslateService translateService;

    public TranslateController(TranslateService translateService) {
        this.translateService = translateService;
    }

    @GetMapping
    public String translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "英文") String targetLanguage
    ) {
        return translateService.translate(text, targetLanguage);
    }
}