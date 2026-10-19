package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 最简单的一个chat使用
 */
@RestController
@RequestMapping("/simpleChat")
public class SimpleChatController {

    private final ChatClient chatClient;

    // ChatClient.Builder 由 Spring AI 自动注册，直接注入即可
    public SimpleChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

}