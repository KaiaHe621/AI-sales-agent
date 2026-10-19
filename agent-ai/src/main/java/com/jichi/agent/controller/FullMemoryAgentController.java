package com.jichi.agent.controller;

import com.jichi.agent.service.FullMemoryAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/full-memory")
public class FullMemoryAgentController {

    private final FullMemoryAgent agent;

    public FullMemoryAgentController(FullMemoryAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public String chat(@RequestBody MemoryRequest request) {
        return agent.chat(request.userId(), request.message(), 8);
    }

    record MemoryRequest(String userId, String message) {
    }
}