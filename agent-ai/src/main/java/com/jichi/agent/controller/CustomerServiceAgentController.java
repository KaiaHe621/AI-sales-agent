package com.jichi.agent.controller;

import com.jichi.agent.service.CustomerServiceAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/cs")
public class CustomerServiceAgentController {

    private final CustomerServiceAgent agent;

    public CustomerServiceAgentController(CustomerServiceAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public String chat(@RequestBody CsRequest request) {
        return agent.chat(request.message());
    }

    record CsRequest(String message) {}
}