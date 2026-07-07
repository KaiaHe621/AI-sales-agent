package com.jichi.salesAgent.controller;

import com.jichi.salesAgent.agent.SalesAgent;
import com.jichi.salesAgent.memory.MysqlChatMemoryStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
public class SalesAgentController {

    private final SalesAgent salesAgent;
    private final MysqlChatMemoryStore chatMemoryStore;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("接收请求: sessionId={}, message={}", request.sessionId(), request.message());
        long start = System.currentTimeMillis();

        String reply = salesAgent.chat(request.sessionId(), request.message(), LocalDate.now().toString());

        long duration = System.currentTimeMillis() - start;
        log.info("请求完成: sessionId={}, durationMs={}", request.sessionId(), duration);

        return ResponseEntity.ok(new ChatResponse(request.sessionId(), reply, duration));
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        chatMemoryStore.deleteMessages(sessionId);
        log.info("会话记忆已清除: sessionId={}", sessionId);
        return ResponseEntity.ok(Map.of("message", "会话记忆已清除", "sessionId", sessionId));
    }
}
