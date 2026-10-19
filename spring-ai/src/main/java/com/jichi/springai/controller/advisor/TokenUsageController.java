package com.jichi.springai.controller.advisor;

import com.jichi.springai.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token-usage")
public class TokenUsageController {

    private final ChatClient chatClient;
    private final TokenUsageAdvisor tokenUsageAdvisor;

    public TokenUsageController(ChatClient.Builder builder,
                                TokenUsageAdvisor tokenUsageAdvisor) {
        this.tokenUsageAdvisor = tokenUsageAdvisor;
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(tokenUsageAdvisor)  // 挂载 Token 统计 Advisor
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "anonymous") String userId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("userId", userId))  // 传 userId 给 Advisor
                .call()
                .content();
    }

    /** 查询某用户累计消耗的 Token 数 */
    @GetMapping("/stats")
    public String stats(@RequestParam String userId) {
        long total = tokenUsageAdvisor.getTotalTokens(userId);
        return String.format("用户 %s 累计消耗 Token：%d", userId, total);
    }
}