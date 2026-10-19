package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/options-demo")
public class OptionsDemoController {

    private final ChatClient chatClient;

    public OptionsDemoController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // 创意模式：高 temperature，适合写作、头脑风暴
    @GetMapping("/creative")
    public String creative(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .options(OpenAiChatOptions.builder()
                        .temperature(1.5)
                        .maxTokens(500)
                        .build())
                .call()
                .content();
    }

    // 精确模式：低 temperature，适合代码生成、数据提取
    @GetMapping("/precise")
    public String precise(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .options(OpenAiChatOptions.builder()
                        .temperature(0.1)
                        .maxTokens(1000)
                        .build())
                .call()
                .content();
    }

    // 厂商无关写法：用通用 ChatOptions（不依赖 OpenAI 具体实现）
    @GetMapping("/generic")
    public String generic(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .options(ChatOptions.builder()
                        .temperature(0.8)
                        .maxTokens(1000)
                        .build())
                .call()
                .content();
    }

}