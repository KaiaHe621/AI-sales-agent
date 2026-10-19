package com.jichi.springai.controller.advisor;

import com.jichi.springai.advisor.LoggingAdvisor;
import com.jichi.springai.advisor.RateLimitAdvisor;
import com.jichi.springai.advisor.ContentSafetyAdvisor;
import com.jichi.springai.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/full-advisor")
public class FullAdvisorController {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory chatMemory;
    private final TokenUsageAdvisor tokenUsageAdvisor;

    public FullAdvisorController(
            ChatClient.Builder builder,
            RateLimitAdvisor rateLimitAdvisor,
            ContentSafetyAdvisor contentSafetyAdvisor,
            TokenUsageAdvisor tokenUsageAdvisor) {

        this.tokenUsageAdvisor = tokenUsageAdvisor;
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();

        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(
                        new LoggingAdvisor(),          // order=HIGHEST_PRECEDENCE，最先执行
                        rateLimitAdvisor,              // order=10，限流
                        contentSafetyAdvisor,          // order=5，内容安全
                        tokenUsageAdvisor              // order=LOWEST_PRECEDENCE，最后统计
                )
                .build();
    }

    @GetMapping
    public String chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "default") String conversationId) {

        return chatClient.prompt()
                .user(message)
                // 运行时参数传给各 Advisor
                .advisors(a -> a.param("userId", userId))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }

    /**
     * 查询某用户累计 Token 消耗
     */
    @GetMapping("/token-usage")
    public String tokenUsage(@RequestParam String userId) {
        long total = tokenUsageAdvisor.getTotalTokens(userId);
        return String.format("用户 %s 累计消耗 Token：%d", userId, total);
    }
}