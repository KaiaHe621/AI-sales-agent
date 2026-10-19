package com.jichi.springaialibaba.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiModelConfig {

    /**
     * 主力 ChatClient：DeepSeek，便宜，适合高频调用
     */
    @Bean("primaryChatClient")
    @Primary
    public ChatClient primaryChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个专业的助手")
                .build();
    }

    /**
     * 备用 ChatClient：通义千问，主力挂了时使用
     */
    @Bean("backupChatClient")
    public ChatClient backupChatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一个专业的助手")
                .build();
    }
}