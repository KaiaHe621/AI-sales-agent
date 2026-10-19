package com.jichi.springaialibaba.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QwenChatClientConfig {

    @Bean("customerServiceChatClient")
    public ChatClient customerServiceChatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("""
                        你是一个专业、耐心的电商客服助手。
                        只回答和我们产品、订单相关的问题。
                        回答简洁，不超过 200 字。
                        """)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-plus")
                        .withTemperature(0.3)
                        .build())
                .build();
    }

    @Bean("contentChatClient")
    public ChatClient contentChatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一个资深文案策划，擅长撰写吸引人的营销文案。")
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-max")
                        .withTemperature(0.9)
                        .build())
                .build();
    }

    @Bean("analysisChatClient")
    public ChatClient analysisChatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一个数据分析师，擅长解读数据并给出业务洞察。")
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo")
                        .withTemperature(0.1)
                        .build())
                .build();
    }
}