package com.jichi.agentscope.controller;

import com.jichi.agentscope.service.PersistentChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final PersistentChatService chatService;

    /**
     * 发送消息
     * sessionId 可以是 userId，也可以是更细粒度的会话 ID
     */
    @PostMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> chat(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body
    ) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 不能为空"));
        }

        String reply = chatService.chat(sessionId, message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    /**
     * 清空会话历史（开启新对话）
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        // 删除本地会话文件
        Path sessionFile = Path.of("sessions", sessionId + ".json");
        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException e) {
            log.warn("删除会话文件失败：{}", sessionFile, e);
        }
        return ResponseEntity.noContent().build();
    }
}