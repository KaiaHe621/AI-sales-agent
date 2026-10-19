package com.jichi.langchain4j.service;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Service;

@Service
public class DynamicAssistantFactory {

    private final ChatModel chatModel;

    public DynamicAssistantFactory(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public TenantChatAssistant createForTenant(String systemPrompt) {
        return AiServices.builder(TenantChatAssistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .systemMessageProvider(memoryId -> systemPrompt)
                // ↑ 根据 memoryId（sessionId）动态返回 System Prompt
                .build();
    }
}