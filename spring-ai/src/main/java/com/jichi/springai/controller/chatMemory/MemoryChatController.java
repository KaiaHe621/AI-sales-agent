package com.jichi.springai.controller.chatMemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/memory-chat")
public class MemoryChatController {

    private final ChatClient chatClient;
    // 单独持有 chatMemory 实例，以便在每次请求时按 conversationId 构建 Advisor
    private final MessageWindowChatMemory chatMemory;

    public MemoryChatController(ChatClient.Builder builder) {
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .build();
    }

    /**
     * 多轮对话接口
     * conversationId 用来区分不同的会话
     */
    @GetMapping
    public String chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId) {

        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }
}