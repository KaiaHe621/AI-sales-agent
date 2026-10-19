package com.jichi.agent.controller;

import com.jichi.agent.service.PersonalAssistantAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/assistant")
public class PersonalAssistantController {

    private final PersonalAssistantAgent agent;

    public PersonalAssistantController(PersonalAssistantAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        return agent.chat(request.message());
    }

    record ChatRequest(String message) {}
}