package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/call-demo")
public class CallDemoController {

    private final ChatClient chatClient;

    public CallDemoController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // 同步调用——拿文本
    @GetMapping("/sync")
    public String sync(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    // 同步调用——拿完整响应（含 Token 用量）
    @GetMapping("/token-usage")
    public TokenUsageResponse tokenUsage(@RequestParam String message) {
        ChatResponse response = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();

        Usage usage = response.getMetadata().getUsage();
        return new TokenUsageResponse(
                response.getResult().getOutput().getText(),
                usage.getPromptTokens(),    // 输入 Token
                usage.getCompletionTokens(), // 输出 Token
                usage.getTotalTokens()      // 合计
        );
    }

    // 流式调用——返回 Flux<String>，适合 SSE 打字机效果
    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    record TokenUsageResponse(String content, Integer inputTokens,
                              Integer outputTokens, Integer totalTokens) {
    }
}