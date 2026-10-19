package com.jichi.a2a.client;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final OrchestratorAgent orchestratorAgent;

    public OrchestratorController(OrchestratorAgent orchestratorAgent) {
        this.orchestratorAgent = orchestratorAgent;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String request) {
        return orchestratorAgent.handle(request);
    }
}