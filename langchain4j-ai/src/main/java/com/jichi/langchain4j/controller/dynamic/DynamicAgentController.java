package com.jichi.langchain4j.controller.dynamic;

import com.jichi.langchain4j.model.UserRole;
import com.jichi.langchain4j.service.dynamic.DynamicAgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamic/agent")
public class DynamicAgentController {

    private final DynamicAgentService agentService;

    public DynamicAgentController(DynamicAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public String chat(@RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId,
                       @RequestParam(defaultValue = "GUEST") UserRole role,
                       @RequestParam String message) {
        return agentService.chat(sessionId, role, message);
    }
}