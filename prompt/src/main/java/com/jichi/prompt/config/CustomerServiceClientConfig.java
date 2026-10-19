package com.jichi.prompt.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.constant.CustomerServicePrompts;
import com.jichi.prompt.tools.OrderQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerServiceClientConfig {

    @Bean
    public ChatClient customerServiceClient(
            DashScopeChatModel chatModel, OrderQueryTools orderTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(CustomerServicePrompts.ECOMMERCE_CUSTOMER_SERVICE_SYSTEM)
                .defaultTools(orderTools)
                .build();
    }
}