package com.jichi.agent.controller;

import com.jichi.agent.service.SimpleReActAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/react")
public class ReActController {

    private final SimpleReActAgent agent;

    public ReActController(SimpleReActAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public String run(@RequestBody TaskRequest request) {
        return agent.run(request.task(), 10);
    }

    record TaskRequest(String task) {
    }
}