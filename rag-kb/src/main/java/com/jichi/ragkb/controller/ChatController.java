package com.jichi.ragkb.controller;

import com.jichi.ragkb.dto.ApiResponse;
import com.jichi.ragkb.dto.ChatRequest;
import com.jichi.ragkb.dto.RagResponse;
import com.jichi.ragkb.entity.ChatMessage;
import com.jichi.ragkb.entity.ChatSession;
import com.jichi.ragkb.repository.ChatMessageRepository;
import com.jichi.ragkb.repository.ChatSessionRepository;
import com.jichi.ragkb.security.UserContext;
import com.jichi.ragkb.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final StreamingRagService streamingRagService;
    private final ChatSessionService sessionService;
    private final PermissionService permissionService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    // 专用线程池处理 SSE 推送（避免占用 Tomcat 线程池）
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r);
                t.setName("sse-rag-" + t.getId());
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * 流式问答接口（SSE）。
     * 前端用 EventSource 接收：
     *   const es = new EventSource('/api/v1/chat/stream?sessionId=xxx&kbIds=1,2&question=...')
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String question,
            @RequestParam List<Long> kbIds,
            @RequestParam(required = false) String sessionId) {

        // 校验权限
        kbIds.forEach(permissionService::requireRead);

        // 创建或获取会话
        String sid = sessionService.getOrCreateSession(sessionId, kbIds);

        SseEmitter emitter = new SseEmitter(60_000L);  // 60秒超时

        // 捕获当前 HTTP 线程的用户上下文，传递给 SSE 线程
        Long currentUserId = UserContext.getUserId();
        String currentDeptId = UserContext.getDepartmentId();
        String currentRole = UserContext.getRole();

        sseExecutor.submit(() -> {
            UserContext.set(currentUserId, currentDeptId, currentRole);
            try {
                streamingRagService.streamQuery(question, kbIds, sid, emitter);
            } catch (Exception e) {
                log.error("[Chat] SSE 执行异常：{}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"系统内部错误，请稍后重试\"}"));
                    emitter.complete();
                } catch (IOException ignored) {}
            } finally {
                UserContext.clear();
            }
        });

        return emitter;
    }

    /**
     * 同步问答接口（测试用）。
     */
    @PostMapping
    public RagResponse syncChat(@RequestBody ChatRequest request) {
        request.getKbIds().forEach(permissionService::requireRead);
        String sid = sessionService.getOrCreateSession(request.getSessionId(), request.getKbIds());
        return streamingRagService.syncQuery(request.getQuestion(), request.getKbIds(), sid);
    }

    /** 获取当前用户的会话列表（按最近活跃时间倒序） */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> listSessions() {
        List<ChatSession> sessions = sessionRepository
                .findByUserIdAndIsDeletedFalseOrderByLastActiveAtDesc(UserContext.getUserId());
        return ApiResponse.ok(sessions);
    }

    /** 获取指定会话的消息列表 */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable String sessionId) {
        return ApiResponse.ok(
                messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }
}