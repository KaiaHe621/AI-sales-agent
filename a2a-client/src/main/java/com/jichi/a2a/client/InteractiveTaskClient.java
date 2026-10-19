package com.jichi.a2a.client;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class InteractiveTaskClient {

    private final A2aClient a2aClient;

    public InteractiveTaskClient(A2aClient a2aClient) {
        this.a2aClient = a2aClient;
    }

    /**
     * 支持多轮交互的任务执行
     * @param clarifier 当 Agent 需要追问时调用，入参是 Agent 的问题，返回值是用户的回答
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeWithInteraction(
            String agentUrl,
            String skillId,
            String userInput,
            java.util.function.Function<String, String> clarifier) {

        String taskId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        List<Map<String, Object>> history = new ArrayList<>();
        history.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("type", "text", "text", userInput))
        ));

        while (true) {
            Map<String, Object> task = a2aClient.sendTask(
                    agentUrl, taskId, sessionId, skillId, history);

            Map<String, Object> status = (Map<String, Object>) task.get("status");
            String state = (String) status.get("state");

            if ("completed".equals(state) || "failed".equals(state) || "canceled".equals(state)) {
                return task;
            }

            if ("input-required".equals(state)) {
                Map<String, Object> agentMessage = (Map<String, Object>) status.get("message");
                String question = extractText(agentMessage);
                String userAnswer = clarifier.apply(question);

                history.add(Map.of(
                        "role", "agent",
                        "parts", List.of(Map.of("type", "text", "text", question))
                ));
                history.add(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("type", "text", "text", userAnswer))
                ));
                continue;
            }

            return task;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> message) {
        if (message == null) return "";
        List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
        if (parts == null || parts.isEmpty()) return "";
        return (String) parts.get(0).getOrDefault("text", "");
    }
}