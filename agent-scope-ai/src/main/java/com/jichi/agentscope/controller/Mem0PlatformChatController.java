package com.jichi.agentscope.controller;

import com.jichi.agentscope.service.Mem0PlatformChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mem0-platform")
@RequiredArgsConstructor
public class Mem0PlatformChatController {

    private final Mem0PlatformChatService mem0PlatformChatService;

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestParam String userId,
            @RequestParam String message) {
        String reply = mem0PlatformChatService.chat(userId, message);
        return Map.of("userId", userId, "reply", reply);
    }
}