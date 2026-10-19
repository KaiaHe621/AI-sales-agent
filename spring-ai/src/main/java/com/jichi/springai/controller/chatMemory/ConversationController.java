package com.jichi.springai.controller.chatMemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ConversationController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatClient = builder
                .defaultSystem("""
                        你是一个智能助手。
                        记住用户告诉你的所有信息，在后续对话中灵活运用。
                        回答简洁，除非用户要求详细解释。
                        """)
                .build();
    }

    /**
     * 发送消息
     */
    @PostMapping("/message")
    public MessageResponse sendMessage(@RequestBody MessageRequest request) {
        String reply = chatClient.prompt()
                .user(request.message())
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(request.conversationId())
                        .build())
                .call()
                .content();

        return new MessageResponse(reply, request.conversationId());
    }

    /**
     * 清除对话历史
     */
    @DeleteMapping("/{conversationId}")
    public void clearConversation(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
    }

    record MessageRequest(String conversationId, String message) {
    }

    record MessageResponse(String reply, String conversationId) {
    }
}