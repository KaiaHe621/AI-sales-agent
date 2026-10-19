package com.jichi.a2a.server.skill;

import com.jichi.a2a.server.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReportGenerationSkillHandler implements SkillHandler {

    private final ChatClient chatClient;

    // 记录"暂停中"的任务上下文，等待用户补充信息
    private final Map<String, PendingTaskContext> pendingTasks = new ConcurrentHashMap<>();

    public ReportGenerationSkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个销售报告生成专家，根据提供的参数生成详细的分析报告。")
                .build();
    }

    @Override
    public String skillId() {
        return "generate-sales-report";
    }

    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        AnalysisParams params = extractParams(history);

        // 信息不完整 → 返回 input-required，等待用户补充
        if (params.startDate() == null || params.endDate() == null) {
            pendingTasks.put(taskId, new PendingTaskContext(sessionId, history, params));
            return new Task(
                    taskId, sessionId,
                    new TaskStatus(
                            TaskState.INPUT_REQUIRED,
                            new Message("agent", List.of(Part.text(buildClarifyingQuestion(params)))),
                            Instant.now().toString()
                    ),
                    history, List.of(), Map.of()
            );
        }

        // 信息完整，直接生成报告
        return generateReport(taskId, sessionId, history, params);
    }

    /**
     * 续传任务：Client 追加了新消息后，A2aController 调用此方法继续执行
     */
    public Task continueTask(String taskId, String sessionId, List<Message> updatedHistory) {
        pendingTasks.remove(taskId);
        AnalysisParams params = extractParams(updatedHistory);

        if (params.startDate() == null || params.endDate() == null) {
            // 信息还是不完整，再次 input-required
            pendingTasks.put(taskId, new PendingTaskContext(sessionId, updatedHistory, params));
            return new Task(
                    taskId, sessionId,
                    new TaskStatus(
                            TaskState.INPUT_REQUIRED,
                            new Message("agent", List.of(Part.text(
                                    "还需要补充：" +
                                            (params.startDate() == null ? "开始日期" : "结束日期") +
                                            "，请提供完整的时间范围。"))),
                            Instant.now().toString()
                    ),
                    updatedHistory, List.of(), Map.of()
            );
        }

        return generateReport(taskId, sessionId, updatedHistory, params);
    }

    private Task generateReport(String taskId, String sessionId,
                                List<Message> history, AnalysisParams params) {
        String report = chatClient.prompt()
                .user(String.format(
                        "生成销售报告\n时间范围：%s 到 %s\n地区：%s\n\n请生成详细的销售分析报告。",
                        params.startDate(), params.endDate(),
                        params.region() != null ? params.region() : "全国"))
                .call()
                .content();

        return new Task(
                taskId, sessionId,
                new TaskStatus(TaskState.COMPLETED,
                        new Message("agent", List.of(Part.text(report))),
                        Instant.now().toString()),
                history,
                List.of(new Artifact("销售报告", "生成的销售分析报告", List.of(Part.text(report)))),
                Map.of()
        );
    }

    private AnalysisParams extractParams(List<Message> history) {
        String allText = history.stream()
                .filter(m -> "user".equals(m.role()))
                .flatMap(m -> m.parts().stream())
                .filter(p -> "text".equals(p.type()))
                .map(Part::text)
                .reduce("", (a, b) -> a + " " + b);

        return new AnalysisParams(
                extractDate(allText, "start"),
                extractDate(allText, "end"),
                extractRegion(allText)
        );
    }

    private String extractDate(String text, String type) {
        // 演示用：只识别 "2025年1月"，生产环境换成正则或 LLM 提取
        if (text.contains("2025年1月")) {
            return type.equals("start") ? "2025-01-01" : "2025-01-31";
        }
        return null;
    }

    private String extractRegion(String text) {
        if (text.contains("华东")) return "华东";
        if (text.contains("华南")) return "华南";
        if (text.contains("全国")) return "全国";
        return null;
    }

    private String buildClarifyingQuestion(AnalysisParams params) {
        List<String> missing = new ArrayList<>();
        if (params.startDate() == null) missing.add("开始日期");
        if (params.endDate() == null) missing.add("结束日期");
        if (params.region() == null) missing.add("地区（可选）");

        return "需要补充以下信息才能生成报告：\n" +
                String.join("、", missing) + "\n\n" +
                "请提供这些信息，例如：「2025年1月1日到2025年1月31日，华东地区」";
    }

    record AnalysisParams(String startDate, String endDate, String region) {
    }

    record PendingTaskContext(String sessionId, List<Message> history, AnalysisParams partialParams) {
    }
}