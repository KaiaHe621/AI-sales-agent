package com.jichi.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient customerServiceChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个专业的客服助手，回答简洁友好，遇到不确定的问题要如实说不知道，不要编造答案。")
                .build();
    }

    @Bean
    public ChatClient codingChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个 Java 技术专家，代码示例使用 Java 21 语法，优先推荐 Spring Boot 相关方案。")
                .build();
    }

}