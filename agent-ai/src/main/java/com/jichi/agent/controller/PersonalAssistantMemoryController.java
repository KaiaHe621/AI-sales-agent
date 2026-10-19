package com.jichi.agent.controller;

import com.jichi.agent.service.PersonalAssistantAgentWithMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/assistant/memory")
public class PersonalAssistantMemoryController {

    private final PersonalAssistantAgentWithMemory agent;

    public PersonalAssistantMemoryController(PersonalAssistantAgentWithMemory agent) {
        this.agent = agent;
    }

    @PostMapping
    public String chat(@RequestBody ChatMemoryRequest request) {
        return agent.chat(request.message(), request.sessionId());
    }

    record ChatMemoryRequest(String message, String sessionId) {
    }
}