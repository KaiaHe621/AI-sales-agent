package com.jichi.agentscope.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.mem0.Mem0LongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Mem0PlatformChatService {

    private final DashScopeChatModel model;

    public String chat(String userId, String userMessage) {
        Mem0LongTermMemory longTermMemory = Mem0LongTermMemory.builder()
                .agentName("个性化助手")
                .userId(userId)
                .apiBaseUrl("https://api.mem0.ai")
                .apiKey("m0-xVEdXwAtSDCk1Gz3yEAfkwlBWYHCuiFrub8pLD3i")
                .build();

        ReActAgent agent = ReActAgent.builder()
                .name("个性化助手")
                .model(model)
                .memory(new InMemoryMemory())
                .sysPrompt("你是一个能记住用户偏好的个性化助手。")
                .longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                .build();

        return agent.call(Msg.builder().textContent(userMessage).build())
                .block()
                .getTextContent();
    }
}