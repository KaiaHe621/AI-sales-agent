package com.jichi.prompt.controller;

import com.jichi.prompt.service.I18nPromptService;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/i18n-prompt")
public class I18nPromptController {

    private final I18nPromptService i18nPromptService;

    public I18nPromptController(I18nPromptService i18nPromptService) {
        this.i18nPromptService = i18nPromptService;
    }

    @GetMapping("/load")
    public String loadPrompt(@RequestParam String name,
                             @RequestParam(defaultValue = "zh") String lang) {
        return i18nPromptService.loadPrompt(name, Locale.forLanguageTag(lang));
    }

}