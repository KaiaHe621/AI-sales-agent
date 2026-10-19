package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.service.TenantAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt/tenant")
public class TenantAssistantController {

    private final TenantAssistant tenantAssistant;

    public TenantAssistantController(TenantAssistant tenantAssistant) {
        this.tenantAssistant = tenantAssistant;
    }

    @GetMapping
    public String chat(@RequestParam String company,
                       @RequestParam String scope,
                       @RequestParam String message) {
        return tenantAssistant.chat(company, scope, message);
    }
}