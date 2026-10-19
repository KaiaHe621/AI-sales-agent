package com.jichi.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TranslateService {

    private final ChatClient chatClient;

    public TranslateService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String translate(String text, String targetLanguage) {
        PromptTemplate template = new PromptTemplate("""
                请将下面这段文字翻译成 {targetLanguage}，
                保持原文的语气和风格，不要意译：
                
                {text}
                """);

        Prompt prompt = template.create(Map.of(
                "targetLanguage", targetLanguage,
                "text", text
        ));

        return chatClient.prompt(prompt).call().content();
    }
}