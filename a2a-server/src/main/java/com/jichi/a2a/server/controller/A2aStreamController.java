package com.jichi.a2a.server.controller;

import com.jichi.a2a.server.model.*;
import com.jichi.a2a.server.repository.TaskRepository;
import com.jichi.a2a.server.skill.SkillHandler;
import com.jichi.a2a.server.skill.StreamingSkillHandler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@RestController
public class A2aStreamController {

    private final Map<String, SkillHandler> skillHandlers;
    private final TaskRepository taskRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public A2aStreamController(List<SkillHandler> handlers, TaskRepository taskRepository) {
        this.skillHandlers = new HashMap<>();
        handlers.forEach(h -> skillHandlers.put(h.skillId(), h));
        this.taskRepository = taskRepository;
    }

    /**
     * tasks/sendSubscribe：SSE 流式任务
     * A2A 协议规定：sendSubscribe 通过独立的 HTTP 接口返回 SSE 流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendSubscribe(@RequestBody Map<String, Object> rpcRequest) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) rpcRequest.get("params");
        String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());
        String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
        String skillId = (String) params.get("skillId");
        String requestId = (String) rpcRequest.get("id");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawHistory = (List<Map<String, Object>>) params.get("history");
        List<Message> history = parseHistory(rawHistory);

        // 异步执行，防止阻塞请求线程
        executor.submit(() -> runStreamingTask(emitter, taskId, sessionId, skillId, history, requestId));

        return emitter;
    }

    private void runStreamingTask(SseEmitter emitter, String taskId, String sessionId,
                                  String skillId, List<Message> history, String requestId) {
        try {
            // 推送：submitted 状态
            sendStatusEvent(emitter, taskId, TaskState.SUBMITTED,
                    "任务已提交，等待处理中...", requestId, false);

            // 推送：working 状态
            sendStatusEvent(emitter, taskId, TaskState.WORKING,
                    "正在处理中...", requestId, false);

            SkillHandler handler = skillHandlers.get(skillId);
            if (handler == null) {
                sendStatusEvent(emitter, taskId, TaskState.FAILED,
                        "未知 Skill: " + skillId, requestId, true);
                return;
            }

            // 如果 Skill 支持流式，传入 emitter 让它主动推中间状态
            if (handler instanceof StreamingSkillHandler streamingHandler) {
                streamingHandler.handleStreaming(taskId, sessionId, history, emitter, requestId);
            } else {
                // 普通 Skill 直接执行，完成后推 completed
                Task result = handler.handle(taskId, sessionId, history);
                taskRepository.save(result);

                // 推送 artifact 事件
                if (!result.artifacts().isEmpty()) {
                    sendArtifactEvent(emitter, taskId, result.artifacts().get(0), requestId, false);
                }

                // 推送 completed 状态
                sendStatusEvent(emitter, taskId, TaskState.COMPLETED,
                        extractAnswerText(result), requestId, true);
            }

        } catch (Exception e) {
            try {
                sendStatusEvent(emitter, taskId, TaskState.FAILED,
                        "执行出错: " + e.getMessage(), requestId, true);
            } catch (Exception ignored) {
            }
            emitter.completeWithError(e);
        }
    }

    private void sendStatusEvent(SseEmitter emitter, String taskId, TaskState state,
                                 String message, String requestId, boolean isFinal) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "jsonrpc", "2.0",
                    "id", requestId,
                    "result", Map.of(
                            "id", taskId,
                            "status", Map.of(
                                    "state", state.name().toLowerCase(),
                                    "message", Map.of(
                                            "role", "agent",
                                            "parts", List.of(Map.of("type", "text", "text", message))
                                    ),
                                    "timestamp", Instant.now().toString()
                            ),
                            "final", isFinal
                    )
            );

            emitter.send(SseEmitter.event()
                    .name("task_status_update")
                    .data(statusUpdate));

            if (isFinal) {
                emitter.complete();
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendArtifactEvent(SseEmitter emitter, String taskId,
                                   Artifact artifact, String requestId, boolean isFinal) {
        try {
            Map<String, Object> artifactEvent = Map.of(
                    "jsonrpc", "2.0",
                    "id", requestId,
                    "result", Map.of(
                            "id", taskId,
                            "artifact", artifact,
                            "final", isFinal
                    )
            );
            emitter.send(SseEmitter.event()
                    .name("task_artifact_update")
                    .data(artifactEvent));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private String extractAnswerText(Task task) {
        if (task.status().message() == null) return "任务完成";
        return task.status().message().parts().stream()
                .filter(p -> "text".equals(p.type()))
                .map(Part::text)
                .findFirst().orElse("任务完成");
    }

    @SuppressWarnings("unchecked")
    private List<Message> parseHistory(List<Map<String, Object>> rawHistory) {
        if (rawHistory == null) return List.of();
        return rawHistory.stream().map(raw -> {
            String role = (String) raw.get("role");
            List<Map<String, Object>> rawParts = (List<Map<String, Object>>) raw.get("parts");
            List<Part> parts = rawParts.stream().map(p -> {
                String type = (String) p.get("type");
                return "data".equals(type)
                        ? Part.data((Map<String, Object>) p.get("data"))
                        : Part.text((String) p.get("text"));
            }).toList();
            return new Message(role, parts);
        }).toList();
    }
}