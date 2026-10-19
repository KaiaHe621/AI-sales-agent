package com.jichi.springai.controller.advisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/advisor-demo")
public class AdvisorDemoController {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory chatMemory;

    public AdvisorDemoController(ChatClient.Builder builder) {
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),   // 打印请求/响应日志，开发调试用
                        MessageChatMemoryAdvisor.builder(chatMemory).build()  // 对话记忆
                )
                .build();
    }

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