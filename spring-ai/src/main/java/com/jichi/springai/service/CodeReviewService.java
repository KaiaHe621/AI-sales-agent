package com.jichi.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CodeReviewService {

    private final ChatClient chatClient;

    public CodeReviewService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String review(String language, String code) {
        PromptTemplate template = new PromptTemplate(
                new ClassPathResource("prompts/code-review.st")
        );

        Prompt prompt = template.create(Map.of(
                "language", language,
                "years", "10",
                "code", code
        ));

        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}