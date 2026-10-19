package com.jichi.springai.controller.advisor;

import com.jichi.springai.advisor.ContentSafetyAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content-safety")
public class ContentSafetyController {

    private final ChatClient chatClient;

    public ContentSafetyController(ChatClient.Builder builder,
                                   ContentSafetyAdvisor contentSafetyAdvisor) {
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(contentSafetyAdvisor)  // 挂载内容安全 Advisor
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