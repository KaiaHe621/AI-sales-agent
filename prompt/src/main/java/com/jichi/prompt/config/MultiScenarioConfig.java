package com.jichi.prompt.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiScenarioConfig {

    @Bean("customerServiceClientNew")
    public ChatClient customerServiceClientNew(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是电商客服助手，只回答订单、物流、退款相关问题。
                        涉及投诉时主动提出转人工。
                        """)
                .build();
    }

    @Bean("codeReviewClient")
    public ChatClient codeReviewClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是资深 Java 工程师，专门做代码 review。
                        找出 Bug、性能问题、最佳实践违反，每个问题标注严重程度。
                        """)
                .build();
    }
}