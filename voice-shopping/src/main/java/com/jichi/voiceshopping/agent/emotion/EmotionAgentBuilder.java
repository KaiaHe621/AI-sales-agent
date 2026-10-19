package com.jichi.voiceshopping.agent.emotion;

import com.jichi.voiceshopping.agent.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmotionAgentBuilder {

    @Qualifier("mainChatModel")   // 主模型，文字质量要求高
    private final Model mainModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("emotion_agent")
                .model(mainModel)
                .sysPrompt(prompts.load("prompts/emotion-merged.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}