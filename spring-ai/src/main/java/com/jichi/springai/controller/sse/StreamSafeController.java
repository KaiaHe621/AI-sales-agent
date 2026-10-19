package com.jichi.springai.controller.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/stream/safe")
public class StreamSafeController {

    private static final Logger log = LoggerFactory.getLogger(StreamSafeController.class);
    private final ChatClient chatClient;

    public StreamSafeController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                // 超时控制：30 秒内没有新数据就触发 TimeoutException
                .timeout(Duration.ofSeconds(30))
                // 超时时推送提示后结束流
                .onErrorResume(TimeoutException.class,
                        e -> Flux.just("[响应超时，请重试]"))
                // 其他异常统一处理
                .onErrorResume(e -> {
                    log.error("流式调用出错: {}", e.getMessage());
                    return Flux.just("[抱歉，生成过程中出现错误，请稍后重试]");
                });
    }
}