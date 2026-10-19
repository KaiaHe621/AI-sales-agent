package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.event.VoiceEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/event")
@RequiredArgsConstructor
public class EventDebugController {

    private final VoiceEventPublisher publisher;

    @PostMapping("/user-spoken")
    public Map<String, Object> userSpoken(@RequestParam String sessionId,
                                          @RequestParam Long userId,
                                          @RequestBody Map<String, String> body) {
        publisher.publishUserSpoken(sessionId, userId, body.getOrDefault("utterance", ""));
        return Map.of("ok", true);
    }
}