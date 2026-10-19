package com.jichi.prompt.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleChatClientConfig {

    @Bean("guestClient")
    public ChatClient guestClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个功能受限的演示助手。
                        只能回答平台介绍和基本使用说明，不处理任何实际业务请求。
                        如需更多功能，引导用户注册会员。
                        """)
                .build();
    }

    @Bean("memberClient")
    public ChatClient memberClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是鸡翅平台的智能助手，帮助会员解答各类问题。
                        按平台规则处理请求，不支持越权操作。
                        """)
                .build();
    }
}