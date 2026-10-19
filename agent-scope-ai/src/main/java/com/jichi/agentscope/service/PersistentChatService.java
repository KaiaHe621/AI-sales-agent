package com.jichi.agentscope.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.SessionManager;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class PersistentChatService {

    private final DashScopeChatModel model;

    public PersistentChatService(DashScopeChatModel model) {
        this.model = model;
    }

    public String chat(String sessionId, String userMessage) {
        InMemoryMemory memory = new InMemoryMemory();

        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .model(model)
                .sysPrompt("你是一个助手。")
                .memory(memory)
                .build();

        // 创建 SessionManager：关联 sessionId、存储路径、需要持久化的组件
        SessionManager sessionManager = SessionManager
                .forSessionId(sessionId)
                .withSession(new JsonSession(Path.of("sessions")))  // JSON 文件存储
                .addComponent(agent)    // 持久化 Agent 状态
                .addComponent(memory);  // 持久化记忆

        // 尝试加载已有会话（如果存在则恢复对话历史）
        sessionManager.loadIfExists();

        // 处理消息
        Msg response = agent.call(
                Msg.builder().textContent(userMessage).build()
        ).block();

        // 保存会话状态
        sessionManager.saveSession();

        return response.getTextContent();
    }
}