package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.jichi.prompt.constant.CustomerServicePrompts.ECOMMERCE_CUSTOMER_SERVICE_SYSTEM;

@Service
public class CustomerServiceChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public CustomerServiceChatService(DashScopeChatModel chatModel) {
        // Spring AI 1.1.x：InMemoryChatMemory 已移除
        // 改用 MessageWindowChatMemory + InMemoryChatMemoryRepository
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(ECOMMERCE_CUSTOMER_SERVICE_SYSTEM)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 处理用户消息
     *
     * @param sessionId 会话 ID（同一用户同一会话用相同 sessionId）
     * @param userMessage 用户消息
     */
    public ChatResponse chat(String sessionId, String userMessage) {
        String response = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,   // 1.1.x 用 ChatMemory.CONVERSATION_ID
                        sessionId))
                .call()
                .content();

        boolean needsHuman = shouldTransferToHuman(userMessage, response);
        return new ChatResponse(response, needsHuman);
    }

    private boolean shouldTransferToHuman(String userMessage, String botResponse) {
        List<String> escalationKeywords = List.of("投诉", "曝光", "消费者协会", "315", "退款不处理");
        boolean hasEscalation = escalationKeywords.stream()
                .anyMatch(userMessage::contains);

        boolean botSuggestsTransfer = botResponse.contains("人工客服") ||
                botResponse.contains("人工为您");

        return hasEscalation || botSuggestsTransfer;
    }

    public record ChatResponse(String reply, boolean needsHumanTransfer) {}
}