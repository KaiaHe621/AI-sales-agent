package com.jichi.springai.controller.prompt;

import com.jichi.springai.service.InlineTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inline")
public class InlineTemplateController {

    private final InlineTemplateService inlineTemplateService;

    public InlineTemplateController(InlineTemplateService inlineTemplateService) {
        this.inlineTemplateService = inlineTemplateService;
    }

    @GetMapping("/ask")
    public String ask(
            @RequestParam String role,
            @RequestParam String domain,
            @RequestParam String concept
    ) {
        return inlineTemplateService.ask(role, domain, concept);
    }
}