package com.jichi.agent.controller;

import com.jichi.agent.service.PlanAndExecuteAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/plan")
public class PlanAndExecuteController {

    private final PlanAndExecuteAgent agent;

    public PlanAndExecuteController(PlanAndExecuteAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public PlanAndExecuteAgent.ExecutionResult run(@RequestBody TaskRequest request) {
        return agent.run(request.task());
    }

    record TaskRequest(String task) {}
}