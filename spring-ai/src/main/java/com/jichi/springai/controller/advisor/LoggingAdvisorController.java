package com.jichi.springai.controller.advisor;

import com.jichi.springai.advisor.LoggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logging-advisor")
public class LoggingAdvisorController {

    private final ChatClient chatClient;

    public LoggingAdvisorController(ChatClient.Builder builder, LoggingAdvisor loggingAdvisor) {
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(loggingAdvisor)   // 注册日志 Advisor
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}