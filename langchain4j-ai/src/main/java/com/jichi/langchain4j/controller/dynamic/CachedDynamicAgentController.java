package com.jichi.langchain4j.controller.dynamic;

import com.jichi.langchain4j.model.UserRole;
import com.jichi.langchain4j.service.dynamic.CachedDynamicAgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamic/agent/cached")
public class CachedDynamicAgentController {

    private final CachedDynamicAgentService agentService;

    public CachedDynamicAgentController(CachedDynamicAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public String chat(@RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId,
                       @RequestParam(defaultValue = "GUEST") UserRole role,
                       @RequestParam String message) {
        return agentService.chat(sessionId, role, message);
    }
}