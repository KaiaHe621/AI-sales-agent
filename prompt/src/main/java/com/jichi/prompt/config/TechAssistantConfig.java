package com.jichi.prompt.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TechAssistantConfig {

    @Bean
    public ChatClient techAssistantClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个 Java 技术助手。
                        只回答 Java 技术相关问题，不确定的内容说不知道，代码用 Java 17 语法。
                        """)
                .build();
    }
}