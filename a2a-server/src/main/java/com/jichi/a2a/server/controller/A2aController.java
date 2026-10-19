package com.jichi.a2a.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.a2a.server.executor.AsyncTaskExecutor;
import com.jichi.a2a.server.model.*;
import com.jichi.a2a.server.registry.WebhookRegistry;
import com.jichi.a2a.server.repository.TaskRepository;
import com.jichi.a2a.server.skill.ReportGenerationSkillHandler;
import com.jichi.a2a.server.skill.SkillHandler;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
public class A2aController {

    private final Map<String, SkillHandler> skillHandlers;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final WebhookRegistry webhookRegistry;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public A2aController(
            List<SkillHandler> handlers,
            TaskRepository taskRepository,
            ObjectMapper objectMapper,
            WebhookRegistry webhookRegistry,
            AsyncTaskExecutor asyncTaskExecutor) {

        this.skillHandlers = new HashMap<>();
        handlers.forEach(h -> skillHandlers.put(h.skillId(), h));
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
        this.webhookRegistry = webhookRegistry;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @PostMapping(value = "/", consumes = "application/json", produces = "application/json")
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request) {
        return switch (request.method()) {
            case "tasks/send" -> handleTaskSend(request);
            case "tasks/get" -> handleTaskGet(request);
            case "tasks/cancel" -> handleTaskCancel(request);
            case "tasks/pushNotification/set" -> handleSetPushNotification(request);
            default -> JsonRpcResponse.error(-32601, "Method not found: " + request.method(), request.id());
        };
    }


    private JsonRpcResponse handleSetPushNotification(JsonRpcRequest request) {
        Map<String, Object> params = request.params();
        String taskId = (String) params.get("id");

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) params.get("pushNotification");
        String callbackUrl = (String) config.get("url");
        String secret = (String) config.getOrDefault("token", null);

        webhookRegistry.register(taskId, callbackUrl, secret);

        return JsonRpcResponse.success(Map.of(
                "id", taskId,
                "pushNotification", Map.of("url", callbackUrl)
        ), request.id());
    }


    private JsonRpcResponse handleTaskSend(JsonRpcRequest request) {
        Map<String, Object> params = request.params();
        String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());
        String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
        String skillId = (String) params.get("skillId");

        List<Message> history = parseHistory(
                (List<Map<String, Object>>) params.get("history"));

        // 检查是否续传任务
        Task existingTask = taskRepository.findById(taskId);
        boolean isContinuation = existingTask != null &&
                TaskState.INPUT_REQUIRED.equals(existingTask.status().state());

        // 续传任务走同步（input-required 场景不适合异步）
        if (isContinuation) {
            SkillHandler handler = skillHandlers.get(skillId);
            if (handler == null) {
                return JsonRpcResponse.error(-32602, "Unknown skill: " + skillId, request.id());
            }
            if (handler instanceof ReportGenerationSkillHandler reportHandler) {
                Task result = reportHandler.continueTask(taskId, sessionId, history);
                taskRepository.save(result);
                return JsonRpcResponse.success(result, request.id());
            }
        }

        // 新任务：交给 AsyncTaskExecutor 异步执行，立即返回 submitted 状态
        // 任务完成后 AsyncTaskExecutor 会自动向注册的 Webhook 地址发回调
        try {
            Task submitted = asyncTaskExecutor.submitAsync(taskId, sessionId, skillId, history);
            return JsonRpcResponse.success(submitted, request.id());
        } catch (IllegalArgumentException e) {
            return JsonRpcResponse.error(-32602, e.getMessage(), request.id());
        }
    }

    private JsonRpcResponse handleTaskGet(JsonRpcRequest request) {
        String taskId = (String) request.params().get("id");
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return JsonRpcResponse.error(-32602, "Task not found: " + taskId, request.id());
        }

        return JsonRpcResponse.success(task, request.id());
    }

    private JsonRpcResponse handleTaskCancel(JsonRpcRequest request) {
        String taskId = (String) request.params().get("id");
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return JsonRpcResponse.error(-32602, "Task not found: " + taskId, request.id());
        }

        Task canceledTask = new Task(
                task.id(), task.sessionId(),
                new TaskStatus(TaskState.CANCELED, null, Instant.now().toString()),
                task.history(), task.artifacts(), task.metadata()
        );
        taskRepository.save(canceledTask);

        return JsonRpcResponse.success(canceledTask, request.id());
    }

    @SuppressWarnings("unchecked")
    private List<Message> parseHistory(List<Map<String, Object>> rawHistory) {
        if (rawHistory == null) return List.of();
        return rawHistory.stream().map(raw -> {
            String role = (String) raw.get("role");
            List<Map<String, Object>> rawParts = (List<Map<String, Object>>) raw.get("parts");
            List<Part> parts = rawParts.stream().map(p -> {
                String type = (String) p.get("type");
                return switch (type) {
                    case "text" -> Part.text((String) p.get("text"));
                    case "data" -> Part.data((Map<String, Object>) p.get("data"));
                    default -> Part.text("");
                };
            }).toList();
            return new Message(role, parts);
        }).toList();
    }
}