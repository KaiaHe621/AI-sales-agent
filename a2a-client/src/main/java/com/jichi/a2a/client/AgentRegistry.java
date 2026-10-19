package com.jichi.a2a.client;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRegistry {

    private final A2aClient a2aClient;

    // key: Agent 名称，value: Agent 信息（Card + URL）
    private final Map<String, AgentInfo> agents = new ConcurrentHashMap<>();

    public AgentRegistry(A2aClient a2aClient) {
        this.a2aClient = a2aClient;
    }

    /**
     * 注册一个 Agent（启动时或动态注册）
     */
    public void register(String agentUrl) {
        Map<String, Object> card = a2aClient.getAgentCard(agentUrl);
        String name = (String) card.get("name");
        agents.put(name, new AgentInfo(name, agentUrl, card));
    }

    /**
     * 获取所有 Agent 的能力摘要（用于喂给 LLM 做编排判断）
     */
    public String buildAgentsSummary() {
        StringBuilder sb = new StringBuilder("可用的 Agent 列表：\n\n");
        agents.forEach((name, info) -> {
            sb.append("## ").append(name).append("\n");
            sb.append("URL: ").append(info.url()).append("\n");
            sb.append("描述: ").append(info.card().get("description")).append("\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> skills = (List<Map<String, Object>>) info.card().get("skills");
            if (skills != null) {
                sb.append("Skills:\n");
                skills.forEach(skill ->
                        sb.append("  - [").append(skill.get("id")).append("] ")
                                .append(skill.get("name")).append(": ")
                                .append(skill.get("description")).append("\n")
                );
            }
            sb.append("\n");
        });
        return sb.toString();
    }

    public AgentInfo getAgent(String name) {
        return agents.get(name);
    }

    public Collection<AgentInfo> getAllAgents() {
        return agents.values();
    }

    public record AgentInfo(String name, String url, Map<String, Object> card) {
    }
}