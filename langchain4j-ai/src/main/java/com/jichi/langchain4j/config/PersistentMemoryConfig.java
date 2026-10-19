package com.jichi.langchain4j.config;

import com.jichi.langchain4j.memory.JpaChatMemoryStore;
import com.jichi.langchain4j.service.chatMemory.PersistentChatAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistentMemoryConfig {

    @Bean
    public PersistentChatAssistant chatAssistant(ChatModel model, JpaChatMemoryStore memoryStore) {
        return AiServices.builder(PersistentChatAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20)
                                .chatMemoryStore(memoryStore)  // 关键：绑定持久化 Store
                                .build())
                .build();
    }
}