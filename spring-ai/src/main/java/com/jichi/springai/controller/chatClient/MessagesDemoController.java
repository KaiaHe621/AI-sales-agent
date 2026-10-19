package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/messages-demo")
public class MessagesDemoController {

    private final ChatClient chatClient;

    public MessagesDemoController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 手动构造多轮消息，演示模型如何"记住"上下文
     *
     * POST /messages-demo
     * {
     *   "lastAssistantReply": "Spring Boot 是基于 Spring 的快速开发框架，通过自动配置简化了项目搭建",
     *   "currentQuestion": "它和 Spring 框架有什么区别"
     * }
     *
     * 模型收到的完整消息列表：
     *   [system] 你是一个 Java 技术助手
     *   [user]   什么是 Spring Boot
     *   [assistant] Spring Boot 是基于 Spring 的快速开发框架……  ← 上一轮历史
     *   [user]   它和 Spring 框架有什么区别                      ← 当前问题
     *
     * 有了前面的历史，模型知道"它"指的是 Spring Boot，能正确回答
     * 如果没有历史，只发"它和 Spring 框架有什么区别"，模型根本不知道"它"是谁
     */
    @PostMapping
    public String chat(@RequestBody ChatHistoryRequest request) {
        List<Message> messages = List.of(
                new SystemMessage("你是一个 Java 技术助手"),
                new UserMessage("什么是 Spring Boot"),           // 第一轮用户问题
                new AssistantMessage(request.lastAssistantReply()), // 第一轮模型回答
                new UserMessage(request.currentQuestion())          // 第二轮用户问题（当前）
        );

        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }

    record ChatHistoryRequest(String lastAssistantReply, String currentQuestion) {}
}