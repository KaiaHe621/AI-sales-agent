package com.jichi.langchain4j.controller.chatMemory;

import com.jichi.langchain4j.service.chatMemory.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/memory/chat")
public class ChatMemoryController {

    private final ChatService chatService;

    public ChatMemoryController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 多轮对话接口
     * X-Session-Id Header 用来标识会话，相同值共享对话历史
     */
    @PostMapping
    public Map<String, String> chat(
            @RequestBody ChatRequest req,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        String reply = chatService.chat(sessionId, req.message());
        return Map.of(
                "sessionId", sessionId,
                "reply", reply
        );
    }

    /**
     * 开始新会话：生成新 sessionId，等同于清空历史
     */
    @PostMapping("/new-session")
    public Map<String, String> newSession() {
        String newSessionId = UUID.randomUUID().toString();
        return Map.of("sessionId", newSessionId);
    }

    record ChatRequest(String message) {}
}