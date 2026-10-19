package com.jichi.springai.controller.chatMemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/long-chat")
public class LongChatController {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory chatMemory;

    public LongChatController(ChatClient.Builder builder) {
        // 保留最近 20 条消息
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .build();
    }

    @GetMapping
    public String chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId
    ) {
        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }
}