package com.jichi.voiceshopping.agent.intent;

import com.jichi.voiceshopping.agent.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntentAgentBuilder {

    @Qualifier("lightChatModel")
    private final Model lightChatModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        String sys = prompts.load("prompts/intent.txt");
        return ReActAgent.builder()
                .name("intent_agent")
                .model(lightChatModel)
                .sysPrompt(sys)
                .memory(new InMemoryMemory())
                .build();
    }
}