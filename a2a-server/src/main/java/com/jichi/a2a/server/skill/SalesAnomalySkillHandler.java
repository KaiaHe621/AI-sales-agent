package com.jichi.a2a.server.skill;

import com.jichi.a2a.server.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class SalesAnomalySkillHandler implements SkillHandler {

    private final ChatClient chatClient;

    public SalesAnomalySkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个销售异常分析专家。
                        分析给定的销售数据，识别异常情况，找出可能的原因，给出处置建议。
                        输出结构：1.异常情况描述 2.可能原因 3.建议措施
                        """)
                .build();
    }

    @Override
    public String skillId() {
        return "analyze-sales-anomaly";
    }

    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        String userQuery = extractLastUserMessage(history);

        String anomalyData = """
                异常数据：
                昨日 14:00-16:00 期间：
                - 销售额：¥12,000（正常时段均值 ¥45,000，下降 73%）
                - 订单转化率：1.2%（正常 4.5%，下降 73%）
                - 页面 PV 正常（说明流量没问题）
                - 支付失败率：38%（正常 < 2%）
                """;

        String analysis = chatClient.prompt()
                .user("请分析以下异常情况：\n" + anomalyData + "\n\n" + userQuery)
                .call()
                .content();

        Map<String, Object> structuredResult = Map.of(
                "anomalyPeriod", "2025-04-15 14:00-16:00",
                "severity", "HIGH",
                "analysis", analysis
        );

        Artifact artifact = new Artifact(
                "异常分析报告",
                "销售异常检测分析结果",
                List.of(Part.text(analysis), Part.data(structuredResult))
        );

        TaskStatus status = new TaskStatus(
                TaskState.COMPLETED,
                new Message("agent", List.of(Part.text(analysis))),
                Instant.now().toString()
        );

        return new Task(taskId, sessionId, status, history, List.of(artifact), Map.of());
    }

    private String extractLastUserMessage(List<Message> history) {
        return history.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(m -> m.parts().stream()
                        .filter(p -> "text".equals(p.type()))
                        .map(Part::text)
                        .findFirst().orElse(""))
                .orElse("");
    }
}