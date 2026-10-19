package com.jichi.a2a.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrchestratorAgent {

    private final ChatClient chatClient;
    private final AgentRegistry agentRegistry;
    private final A2aClient a2aClient;

    public OrchestratorAgent(
            ChatClient.Builder builder,
            AgentRegistry agentRegistry,
            A2aClient a2aClient) {
        this.agentRegistry = agentRegistry;
        this.a2aClient = a2aClient;
        this.chatClient = builder.build();
    }

    /**
     * 处理用户请求：LLM 决定路由，委托给专业 Agent
     */
    public String handle(String userRequest) {
        // 第一步：让 LLM 分析用户意图，决定调哪个 Agent 的哪个 Skill
        String agentsSummary = agentRegistry.buildAgentsSummary();

        String routingDecision = chatClient.prompt()
                .system("""
                        你是一个任务路由器。根据用户请求和可用 Agent 列表，
                        决定应该调用哪个 Agent 的哪个 Skill。
                        
                        只输出 JSON，格式：
                        {"agentName": "Agent名称", "skillId": "skill-id", "reason": "选择原因"}
                        
                        如果没有合适的 Agent，输出：
                        {"agentName": null, "skillId": null, "reason": "原因"}
                        """)
                .user("可用 Agent：\n" + agentsSummary + "\n\n用户请求：" + userRequest)
                .call()
                .content();

        // 解析路由决策
        RoutingDecision decision = parseDecision(routingDecision);

        if (decision.agentName() == null) {
            return "抱歉，没有找到能处理这个请求的 Agent：" + decision.reason();
        }

        // 第二步：委托给对应 Agent 执行
        AgentRegistry.AgentInfo agentInfo = agentRegistry.getAgent(decision.agentName());
        if (agentInfo == null) {
            return "Agent 不存在：" + decision.agentName();
        }

        String taskId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        List<Map<String, Object>> history = List.of(
                Map.of("role", "user",
                        "parts", List.of(Map.of("type", "text", "text", userRequest)))
        );

        Map<String, Object> taskResult = a2aClient.sendTask(
                agentInfo.url(), taskId, sessionId, decision.skillId(), history);

        // 第三步：提取结果
        return extractResultText(taskResult);
    }

    @SuppressWarnings("unchecked")
    private String extractResultText(Map<String, Object> task) {
        Map<String, Object> status = (Map<String, Object>) task.get("status");
        Map<String, Object> message = (Map<String, Object>) status.get("message");
        if (message == null) return "任务完成，但无文本输出";

        List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
        return parts.stream()
                .filter(p -> "text".equals(p.get("type")))
                .map(p -> (String) p.get("text"))
                .findFirst()
                .orElse("无文本输出");
    }

    private RoutingDecision parseDecision(String json) {
        // 简单解析，生产用 ObjectMapper
        json = json.trim().replaceAll("```json|```", "").trim();
        try {
            boolean hasAgent = json.contains("\"agentName\": \"") &&
                    !json.contains("\"agentName\": null");
            if (!hasAgent) return new RoutingDecision(null, null, "未找到合适 Agent");

            String agentName = extractJsonField(json, "agentName");
            String skillId = extractJsonField(json, "skillId");
            String reason = extractJsonField(json, "reason");
            return new RoutingDecision(agentName, skillId, reason);
        } catch (Exception e) {
            return new RoutingDecision(null, null, "解析失败");
        }
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\": \"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return start > key.length() - 1 && end > start ? json.substring(start, end) : null;
    }

    record RoutingDecision(String agentName, String skillId, String reason) {
    }
}