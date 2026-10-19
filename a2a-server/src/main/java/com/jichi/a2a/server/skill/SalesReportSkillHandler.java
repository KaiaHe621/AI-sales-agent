package com.jichi.a2a.server.skill;

import com.jichi.a2a.server.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

//@Component
public class SalesReportSkillHandler implements StreamingSkillHandler {

    private final ChatClient chatClient;

    public SalesReportSkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个专业的销售报告生成助手，生成结构清晰、数据详实的分析报告。")
                .build();
    }

    @Override
    public String skillId() {
        return "generate-sales-report";
    }

    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        // 非流式调用的降级实现
        String report = generateReport(extractUserMessage(history));
        TaskStatus status = new TaskStatus(TaskState.COMPLETED,
                new Message("agent", List.of(Part.text(report))),
                java.time.Instant.now().toString());
        return new Task(taskId, sessionId, status, history,
                List.of(new Artifact("销售报告", "生成的报告", List.of(Part.text(report)))),
                java.util.Map.of());
    }

    @Override
    public void handleStreaming(String taskId, String sessionId, List<Message> history,
                                SseEmitter emitter, String requestId) {
        try {
            String userRequest = extractUserMessage(history);

            // 推送步骤一：数据获取
            pushProgress(emitter, taskId, requestId, "正在获取销售数据...");
            Thread.sleep(1000); // 模拟耗时

            // 推送步骤二：数据分析
            pushProgress(emitter, taskId, requestId, "正在分析数据趋势...");
            Thread.sleep(1500);

            // 推送步骤三：生成报告
            pushProgress(emitter, taskId, requestId, "正在生成报告...");
            String report = generateReport(userRequest);

            // 推送 Artifact
            pushArtifact(emitter, taskId, requestId, report);

            // 推送完成
            pushCompleted(emitter, taskId, requestId, report);

        } catch (Exception e) {
            try {
                pushFailed(emitter, taskId, requestId, e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    private String generateReport(String userRequest) {
        return chatClient.prompt()
                .user("请生成一份销售分析报告，需求：" + userRequest)
                .call()
                .content();
    }

    private void pushProgress(SseEmitter emitter, String taskId, String requestId, String message) {
        sendEvent(emitter, "task_status_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "status", Map.of("state", "working",
                                "message", Map.of("role", "agent",
                                        "parts", List.of(Map.of("type", "text", "text", message))),
                                "timestamp", java.time.Instant.now().toString()),
                        "final", false)));
    }

    private void pushArtifact(SseEmitter emitter, String taskId, String requestId, String content) {
        sendEvent(emitter, "task_artifact_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "artifact", Map.of("name", "销售报告",
                                "parts", List.of(Map.of("type", "text", "text", content))),
                        "final", false)));
    }

    private void pushCompleted(SseEmitter emitter, String taskId, String requestId, String content) {
        sendEvent(emitter, "task_status_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "status", Map.of("state", "completed",
                                "message", Map.of("role", "agent",
                                        "parts", List.of(Map.of("type", "text", "text", content))),
                                "timestamp", java.time.Instant.now().toString()),
                        "final", true)));
        emitter.complete();
    }

    private void pushFailed(SseEmitter emitter, String taskId, String requestId, String error) {
        sendEvent(emitter, "task_status_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "status", Map.of("state", "failed",
                                "message", Map.of("role", "agent",
                                        "parts", List.of(Map.of("type", "text", "text", "执行失败: " + error))),
                                "timestamp", java.time.Instant.now().toString()),
                        "final", true)));
        emitter.complete();
    }

    private void sendEvent(SseEmitter emitter, String eventName, java.util.Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private String extractUserMessage(List<Message> history) {
        return history.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(m -> m.parts().stream()
                        .filter(p -> "text".equals(p.type()))
                        .map(Part::text).findFirst().orElse(""))
                .orElse("");
    }
}