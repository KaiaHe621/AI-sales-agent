package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.dto.ClarifyResult;
import com.jichi.voiceshopping.service.ClarifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class ClarifyDebugController {

    private final ClarifyService clarify;

    @PostMapping("/clarify")
    public ClarifyResult decide(@RequestParam String sessionId,
                                @RequestBody ClarifyDebugReq req) {
        return clarify.decide(sessionId, req.utterance(), req.slots());
    }

    public record ClarifyDebugReq(String utterance, Map<String, Object> slots) {
    }
}