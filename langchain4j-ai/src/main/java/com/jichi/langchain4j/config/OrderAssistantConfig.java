package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.tools.OrderAssistant;
import com.jichi.langchain4j.tools.OrderQueryTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OrderAssistantConfig {

    @Bean
    @Primary
    public OrderAssistant orderAssistantWithTools(ChatModel model, OrderQueryTools orderQueryTools) {
        return AiServices.builder(OrderAssistant.class)
                .chatModel(model)
                .tools(orderQueryTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}