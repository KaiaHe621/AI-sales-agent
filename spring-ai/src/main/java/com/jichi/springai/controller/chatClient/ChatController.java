package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个专业的 Java 技术助手，回答简洁准确，代码示例使用 Java 21 语法。")
                .build();
    }

    /**
     * 基础对话
     * GET /api/chat?message=什么是Spring Boot
     */
    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 临时覆盖 System Prompt
     * POST /api/chat/with-role
     * {"systemPrompt":"你是诗人","userMessage":"写首诗"}
     */
    @PostMapping("/with-role")
    public String chatWithRole(@RequestBody ChatRequest request) {
        return chatClient.prompt()
                .system(request.systemPrompt())
                .user(request.userMessage())
                .call()
                .content();
    }

    /**
     * 模板变量替换
     * GET /api/chat/template?topic=JVM&difficulty=高级
     */
    @GetMapping("/template")
    public String chatWithTemplate(
            @RequestParam String topic,
            @RequestParam(defaultValue = "中级") String difficulty) {
        return chatClient.prompt()
                .user(u -> u.text("请出一道关于 {topic} 的 {difficulty} 难度面试题")
                        .param("topic", topic)
                        .param("difficulty", difficulty))
                .call()
                .content();
    }

    /**
     * 返回完整响应（含 Token 用量）
     * GET /api/chat/detail?message=你好
     */
    @GetMapping("/detail")
    public ChatDetailResponse chatDetail(@RequestParam String message) {
        ChatResponse response = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();

        return new ChatDetailResponse(
                response.getResult().getOutput().getText(),
                response.getMetadata().getUsage().getTotalTokens()
        );
    }

    /**
     * 创意模式（高 temperature）
     * GET /api/chat/creative?message=给我起个公司名
     */
    @GetMapping("/creative")
    public String creativeChat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .options(OpenAiChatOptions.builder()
                        .temperature(1.2)
                        .maxTokens(500)
                        .build())
                .call()
                .content();
    }

    /**
     * 流式输出（打字机效果）
     * GET /api/chat/stream?message=写首诗
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    /**
     * 手动构造多轮消息（演示底层用法，实际项目用 ChatMemory）
     * POST /api/chat/history
     */
    @PostMapping("/history")
    public String chatWithHistory(@RequestBody HistoryRequest request) {
        List<Message> messages = List.of(
                new SystemMessage("你是一个 Java 技术助手"),
                new UserMessage(request.previousQuestion()),
                new AssistantMessage(request.previousAnswer()),
                new UserMessage(request.currentQuestion())
        );
        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }

    // DTO
    record ChatRequest(String systemPrompt, String userMessage) {
    }

    record ChatDetailResponse(String content, Integer totalTokens) {
    }

    record HistoryRequest(String previousQuestion,
                          String previousAnswer,
                          String currentQuestion) {
    }
}