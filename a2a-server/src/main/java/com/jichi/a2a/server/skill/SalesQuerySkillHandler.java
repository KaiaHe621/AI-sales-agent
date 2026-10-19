package com.jichi.a2a.server.skill;

import com.jichi.a2a.server.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class SalesQuerySkillHandler implements SkillHandler {

    private final ChatClient chatClient;

    public SalesQuerySkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个专业的销售数据分析助手。
                        根据用户的问题，分析销售数据并给出清晰的回答。
                        回答要简洁，重点突出关键数据指标。
                        """)
                .build();
    }

    @Override
    public String skillId() {
        return "query-sales-summary";
    }

    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        // 取最后一条用户消息作为查询内容
        String userQuery = history.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((first, second) -> second)
                .map(m -> m.parts().stream()
                        .filter(p -> "text".equals(p.type()))
                        .map(Part::text)
                        .findFirst()
                        .orElse(""))
                .orElse("");

        // 模拟查数据库（实际应注入真实数据服务）
        String salesData = querySalesData();

        // 调用 AI 生成分析回答
        String answer = chatClient.prompt()
                .user("销售数据：\n" + salesData + "\n\n用户问题：" + userQuery)
                .call()
                .content();

        // 构建完成的 Task
        Artifact artifact = new Artifact(
                "销售数据分析结果",
                "根据用户问题生成的销售数据分析",
                List.of(Part.text(answer))
        );

        TaskStatus status = new TaskStatus(
                TaskState.COMPLETED,
                new Message("agent", List.of(Part.text(answer))),
                Instant.now().toString()
        );

        return new Task(taskId, sessionId, status, history, List.of(artifact), Map.of());
    }

    private String querySalesData() {
        // 模拟销售数据
        return """
                本周销售数据：
                - 总销售额：¥1,280,000
                - 订单数：3,420 单
                - 客单价：¥374
                - 环比上周：+8.3%
                - TOP3 品类：电子产品(42%)、服装(28%)、家居(15%)
                """;
    }
}