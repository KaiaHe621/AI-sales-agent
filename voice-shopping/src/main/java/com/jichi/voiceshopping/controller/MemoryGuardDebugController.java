package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.service.AgentMemoryGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug/memory")
@RequiredArgsConstructor
public class MemoryGuardDebugController {

    private final AgentMemoryGuard guard;

    @PostMapping("/trim")
    public Map<String, String> trim() {
        guard.trimAllAgentMemories();
        return Map.of("status", "ok");
    }
}