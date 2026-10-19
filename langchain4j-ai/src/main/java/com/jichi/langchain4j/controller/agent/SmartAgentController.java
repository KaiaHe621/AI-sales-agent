package com.jichi.langchain4j.controller.agent;

import com.jichi.langchain4j.service.agent.SmartAgent;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/agent/loop")
public class SmartAgentController {

    private final SmartAgent agent;

    public SmartAgentController(SmartAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody AgentRequest req) {
        String reply = agent.chat(req.sessionId(), req.message());
        return Map.of("sessionId", req.sessionId(), "reply", reply);
    }

    record AgentRequest(String sessionId, String message) {
    }
}