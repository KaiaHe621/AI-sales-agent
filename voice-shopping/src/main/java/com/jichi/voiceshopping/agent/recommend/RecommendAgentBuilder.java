package com.jichi.voiceshopping.agent.recommend;

import com.jichi.voiceshopping.agent.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendAgentBuilder {

    @Qualifier("mainChatModel")   // 推荐理由是门面文字，必须主模型
    private final Model mainModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("recommend_agent")
                .model(mainModel)
                .sysPrompt(prompts.load("prompts/recommend-reason.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}