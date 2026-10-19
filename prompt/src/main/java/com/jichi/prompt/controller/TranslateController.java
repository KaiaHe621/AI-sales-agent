package com.jichi.prompt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translate")
public class TranslateController {

    private static final String BASE_SYSTEM = "你是一个技术助手，回答简洁准确。";

    private final ChatClient techAssistantClient;

    public TranslateController(ChatClient techAssistantClient) {
        this.techAssistantClient = techAssistantClient;
    }

    @GetMapping
    public String translate(@RequestParam String text, @RequestParam String lang) {
        return techAssistantClient.prompt()
                // 拼接追加，而不是直接覆盖
                .system(BASE_SYSTEM + "\n此外：你是专业翻译，只做翻译，不解释。")
                .user("翻译成 " + lang + "：\n" + text)
                .call()
                .content();
    }
}