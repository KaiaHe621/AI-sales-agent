package com.jichi.a2a.client;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class LongRunningTaskService {

    private final A2aClient a2aClient;
    private final PushNotificationClient pushClient;
    private final TaskResultStore resultStore;

    // Webhook 地址：本地测试用 localhost，Client 跑在 8090，Server 跑在 8080
    // Server 向这个地址发回调，所以必须是 Server 能访问到的地址
    // 本地两个进程：http://localhost:8090/webhook/a2a-callback
    // 生产部署：https://your-domain.com/webhook/a2a-callback
    private static final String MY_WEBHOOK_URL = "http://localhost:8090/webhook/a2a-callback";
    private static final String WEBHOOK_SECRET = System.getenv("WEBHOOK_SECRET");

    public LongRunningTaskService(A2aClient a2aClient,
                                  PushNotificationClient pushClient,
                                  TaskResultStore resultStore) {
        this.a2aClient = a2aClient;
        this.pushClient = pushClient;
        this.resultStore = resultStore;
    }

    /**
     * 提交长任务，拿 Future，等 Webhook 通知触发
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<String> submitLongTask(String question) {
        String taskId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        // 1. 提交任务到 Server（立即返回 submitted 状态）
        a2aClient.sendTask(
                "http://localhost:8080",   // Server 地址
                taskId, sessionId, "generate-sales-report",
                List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("type", "text", "text", question))))
        );

        // 2. 注册 Webhook，告诉 Server 任务完成后回调哪个地址
        pushClient.registerWebhook(
                "http://localhost:8080",   // Server 地址
                taskId,
                MY_WEBHOOK_URL,            // Client 自己对外暴露的 Webhook 接口
                WEBHOOK_SECRET
        );

        // 3. 返回 Future，业务代码可以在合适的时候 await
        return resultStore.waitForResult(taskId)
                .thenApply(result -> extractText((Map<String, Object>) result))
                .orTimeout(30, TimeUnit.MINUTES);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> task) {
        Map<String, Object> status = (Map<String, Object>) task.get("status");
        Map<String, Object> message = (Map<String, Object>) status.get("message");
        if (message == null) return "任务完成";
        var parts = (List<Map<String, Object>>) message.get("parts");
        return parts.isEmpty() ? "" : (String) parts.get(0).getOrDefault("text", "");
    }
}