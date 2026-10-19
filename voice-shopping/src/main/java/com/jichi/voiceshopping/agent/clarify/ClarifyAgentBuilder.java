package com.jichi.voiceshopping.agent.clarify;

import com.jichi.voiceshopping.agent.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClarifyAgentBuilder {

    @Qualifier("lightChatModel")   // 生成追问也不用主模型
    private final Model model;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("clarify_agent")
                .model(model)
                .sysPrompt(prompts.load("prompts/clarify.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}