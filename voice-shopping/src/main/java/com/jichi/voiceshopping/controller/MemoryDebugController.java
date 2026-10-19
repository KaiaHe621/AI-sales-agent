package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.service.LongTermMemoryWriter;
import com.jichi.voiceshopping.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug/memory")
@RequiredArgsConstructor
public class MemoryDebugController {

    private final LongTermMemoryWriter writer;
    private final SessionService sessionService;

    @PostMapping("/flush")
    public Map<String, Object> flush(@RequestParam String sessionId,
                                     @RequestParam(required = false) Long userId) {
        Long uid = userId != null ? userId : sessionService.findUserId(sessionId);
        if (uid == null) {
            return Map.of("ok", false, "reason", "无法解析 userId，请显式传 userId 参数");
        }
        writer.flushOnSessionEnd(sessionId, uid);
        return Map.of("ok", true, "sessionId", sessionId, "userId", uid);
    }
}