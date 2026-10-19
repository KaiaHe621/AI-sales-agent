package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.service.TenantAwareAssistantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant-chat")
public class TenantChatController {

    private final TenantAwareAssistantService assistantService;

    public TenantChatController(TenantAwareAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping
    public String chat(@RequestParam String tenantId,
                       @RequestParam String sessionId,
                       @RequestParam String message) {
        return assistantService.chat(tenantId, sessionId, message);
    }
}