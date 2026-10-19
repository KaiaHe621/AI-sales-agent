package com.jichi.springai.controller.advisor;

import com.jichi.springai.advisor.RateLimitAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

    private final ChatClient chatClient;

    public RateLimitController(ChatClient.Builder builder, RateLimitAdvisor rateLimitAdvisor) {
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(rateLimitAdvisor)
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "anonymous") String userId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("userId", userId))  // 传给 RateLimitAdvisor
                .call()
                .content();
    }
}