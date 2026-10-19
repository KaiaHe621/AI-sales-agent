package com.jichi.springai.controller.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/stream/save")
public class StreamSaveController {

    private final ChatClient chatClient;

    public StreamSaveController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 流式推送给前端，同时在服务端收集完整回复后存库
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatAndSave(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId) {

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> saveToDatabase(conversationId, message, fullResponse.toString()));
    }

    private void saveToDatabase(String conversationId, String question, String answer) {
        // 替换为实际的数据库操作
        log.info("保存对话 conversationId={}, answer长度={}", conversationId, answer.length());
    }
}