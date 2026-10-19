package com.jichi.voiceshopping.config;

import io.agentscope.core.formatter.dashscope.DashScopeMultiAgentFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiAgentModelConfig {

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Bean("multiAgentChatModel")
    public Model multiAgentChatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-plus")
                .formatter(new DashScopeMultiAgentFormatter())
                .build();
    }
}