package com.jichi.a2a.server.executor;

import com.jichi.a2a.server.model.*;
import com.jichi.a2a.server.registry.WebhookRegistry;
import com.jichi.a2a.server.repository.TaskRepository;
import com.jichi.a2a.server.skill.SkillHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class AsyncTaskExecutor {

    private final Map<String, SkillHandler> skillHandlers;
    private final TaskRepository taskRepository;
    private final WebhookRegistry webhookRegistry;
    private final RestTemplate restTemplate;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AsyncTaskExecutor(
            List<SkillHandler> handlers,
            TaskRepository taskRepository,
            WebhookRegistry webhookRegistry,
            RestTemplate restTemplate) {

        this.skillHandlers = new HashMap<>();
        handlers.forEach(h -> skillHandlers.put(h.skillId(), h));
        this.taskRepository = taskRepository;
        this.webhookRegistry = webhookRegistry;
        this.restTemplate = restTemplate;
    }

    /**
     * 提交任务并立即返回（不等执行完成）
     */
    public Task submitAsync(String taskId, String sessionId,
                            String skillId, List<Message> history) {
        SkillHandler handler = skillHandlers.get(skillId);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown skill: " + skillId);
        }

        // 创建 submitted 状态的任务并保存
        Task submitted = new Task(
                taskId, sessionId,
                new TaskStatus(TaskState.SUBMITTED, null, java.time.Instant.now().toString()),
                history, List.of(), Map.of()
        );
        taskRepository.save(submitted);

        // 异步执行
        executor.submit(() -> executeAndNotify(taskId, sessionId, skillId, history, handler));

        return submitted;
    }

    private void executeAndNotify(String taskId, String sessionId,
                                   String skillId, List<Message> history,
                                   SkillHandler handler) {
        try {
            // 更新为 working 状态
            Task working = new Task(
                    taskId, sessionId,
                    new TaskStatus(TaskState.WORKING,
                            new Message("agent", List.of(Part.text("正在处理中..."))),
                            java.time.Instant.now().toString()),
                    history, List.of(), Map.of()
            );
            taskRepository.save(working);

            // 执行 Skill
            Task result = handler.handle(taskId, sessionId, history);
            taskRepository.save(result);

            // 任务完成，发送 Push Notification
            sendPushNotification(taskId, result);

        } catch (Exception e) {
            // 执行失败，更新状态并通知
            Task failed = new Task(
                    taskId, sessionId,
                    new TaskStatus(TaskState.FAILED,
                            new Message("agent", List.of(Part.text("执行失败: " + e.getMessage()))),
                            java.time.Instant.now().toString()),
                    history, List.of(), Map.of()
            );
            taskRepository.save(failed);
            sendPushNotification(taskId, failed);
        }
    }

    private void sendPushNotification(String taskId, Task task) {
        WebhookRegistry.WebhookConfig webhook = webhookRegistry.getWebhook(taskId);
        if (webhook == null) return;

        try {
            Map<String, Object> notification = Map.of(
                    "taskId", taskId,
                    "task", task,
                    "timestamp", java.time.Instant.now().toString()
            );

            // 设置请求头（包含签名，防伪造）
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if (webhook.secret() != null) {
                String signature = generateSignature(task.id(), webhook.secret());
                headers.set("X-A2A-Signature", signature);
            }

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(notification, headers);

            restTemplate.postForEntity(webhook.callbackUrl(), entity, Void.class);

        } catch (Exception e) {
            // Push Notification 失败只记录日志，不影响任务本身的结果
            log.warn("[A2A] Push Notification 发送失败 taskId={}：{}", taskId, e.getMessage());
        }
    }

    private String generateSignature(String taskId, String secret) {
        // HMAC-SHA256 签名，防止 Webhook 被伪造
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(taskId.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}