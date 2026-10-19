package com.jichi.langchain4j.service.chatMemory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {

    private final ChatAssistant assistant;

    public ChatService(ChatModel model) {
        this.assistant = AiServices.builder(ChatAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    public String chat(String sessionId, String message) {
        log.info("会话 [{}] 消息：{}", sessionId, message);
        String response = assistant.chat(sessionId, message);
        log.info("会话 [{}] 回复：{}", sessionId, response);
        return response;
    }
}