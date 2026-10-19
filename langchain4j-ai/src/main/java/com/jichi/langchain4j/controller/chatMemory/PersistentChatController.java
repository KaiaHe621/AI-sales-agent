package com.jichi.langchain4j.controller.chatMemory;

import com.jichi.langchain4j.service.chatMemory.PersistentChatAssistant;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/persistent/chat")
public class PersistentChatController {

    private final PersistentChatAssistant chatAssistant;

    public PersistentChatController(PersistentChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
    }

    @PostMapping
    public Map<String, String> chat(
            @RequestBody ChatRequest req,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        String reply = chatAssistant.chat(sessionId, req.message());
        return Map.of("sessionId", sessionId, "reply", reply);
    }

    record ChatRequest(String message) {}
}