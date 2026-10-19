package com.jichi.agent.service;

import com.jichi.agent.tools.AssistantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PersonalAssistantAgentWithMemory {

    private final ChatClient chatClient;

    public PersonalAssistantAgentWithMemory(@Qualifier("dashScopeChatModel") ChatModel chatModel,
                                             AssistantTools assistantTools) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个聪明的个人助理，名字叫小智。
                        能查天气、告知时间、查汇率、创建提醒。
                        需要数据时调工具，不要猜测和编造。
                        """)
                .defaultTools(assistantTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                        .build())
                                .build())
                .build();
    }

    public String chat(String message, String sessionId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }
}