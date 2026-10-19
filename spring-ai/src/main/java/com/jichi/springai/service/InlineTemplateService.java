package com.jichi.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class InlineTemplateService {

    private final ChatClient chatClient;

    public InlineTemplateService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String role, String domain, String concept) {
        return chatClient.prompt()
                .system(s -> s.text("你是一个 {role}，擅长 {domain} 领域的问题。")
                        .param("role", role)
                        .param("domain", domain))
                .user(u -> u.text("请解释 {concept} 的工作原理")
                        .param("concept", concept))
                .call()
                .content();
    }
}