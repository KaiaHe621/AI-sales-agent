package com.jichi.a2a.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class A2aWebhookController {

    private final TaskResultStore taskResultStore;

    public A2aWebhookController(TaskResultStore taskResultStore) {
        this.taskResultStore = taskResultStore;
    }

    /**
     * 接收 A2A Server 的 Push Notification
     */
    @PostMapping("/a2a-callback")
    public ResponseEntity<Void> receiveNotification(
            @RequestBody Map<String, Object> notification,
            @RequestHeader(value = "X-A2A-Signature", required = false) String signature) {

        System.out.println("收到hook");
        String taskId = (String) notification.get("taskId");

        // 验证签名（防伪造）
        if (!verifySignature(taskId, signature)) {
            return ResponseEntity.status(401).build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> task = (Map<String, Object>) notification.get("task");

        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) task.get("status");
        // toLowerCase() 防止 Server 端枚举未修复时大小写不匹配（如 "COMPLETED" vs "completed"）
        String state = ((String) status.get("state")).toLowerCase();

        log.info("[A2A Webhook] 收到任务 {} 回调，状态：{}", taskId, state);

        // 根据任务状态做不同处理
        switch (state) {
            case "completed" -> {
                taskResultStore.store(taskId, task);
                taskResultStore.notifyWaiters(taskId);
            }
            case "failed" -> {
                taskResultStore.storeFailed(taskId, extractErrorMessage(status));
                taskResultStore.notifyWaiters(taskId);
            }
            default -> {
                log.info("[A2A Webhook] 任务 {} 中间状态：{}", taskId, state);
            }
        }

        return ResponseEntity.ok().build();
    }

    private boolean verifySignature(String taskId, String signature) {
        if (signature == null) return true; // 没有配置签名则跳过验证
        // 实际用 HMAC-SHA256 验证，这里简化
        return true;
    }

    @SuppressWarnings("unchecked")
    private String extractErrorMessage(Map<String, Object> status) {
        Map<String, Object> message = (Map<String, Object>) status.get("message");
        if (message == null) return "未知错误";
        var parts = (java.util.List<Map<String, Object>>) message.get("parts");
        if (parts == null || parts.isEmpty()) return "未知错误";
        return (String) parts.get(0).getOrDefault("text", "未知错误");
    }
}