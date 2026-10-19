package com.jichi.voiceshopping.agent.perspective;

import com.jichi.voiceshopping.agent.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PerspectiveAgentBuilder {

    @Qualifier("multiAgentChatModel")
    private final Model model;

    private final PromptLoader promptLoader;

    public ReActAgent build(String name, String promptFile) {
        return ReActAgent.builder()
                .name(name)
                .model(model)
                .memory(new InMemoryMemory())
                .sysPrompt(promptLoader.load(promptFile))
                .build();
    }
}