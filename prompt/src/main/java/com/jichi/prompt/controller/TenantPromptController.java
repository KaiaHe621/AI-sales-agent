package com.jichi.prompt.controller;

import com.jichi.prompt.entity.TenantConfig;
import com.jichi.prompt.service.TenantPromptService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant-prompt")
public class TenantPromptController {

    private final TenantPromptService tenantPromptService;

    public TenantPromptController(TenantPromptService tenantPromptService) {
        this.tenantPromptService = tenantPromptService;
    }

    @PostMapping("/generate")
    public String generateTenantPrompt(@RequestBody TenantConfig config) {
        return tenantPromptService.generateTenantPrompt(config);
    }
}