package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.dto.EmotionResult;
import com.jichi.voiceshopping.service.OrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatDebugController {

    private final OrchestratorService orchestrator;

    @PostMapping
    public EmotionResult chat(@RequestParam String sessionId,
                              @RequestParam Long userId,
                              @RequestBody String utterance) {
        return orchestrator.handle(sessionId, userId, utterance);
    }
}