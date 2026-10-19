package com.jichi.prompt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tech")
public class TechAssistantController {

    private final ChatClient techAssistantClient;

    public TechAssistantController(ChatClient techAssistantClient) {
        this.techAssistantClient = techAssistantClient;
    }

    @GetMapping
    public String ask(@RequestParam String question) {
        return techAssistantClient.prompt()
                .user(question)
                .call()
                .content();
    }
}