package com.jichi.a2a.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Consumer;

@Component
@Slf4j
public class StreamingA2aClient {

    private final WebClient webClient;

    public StreamingA2aClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /**
     * 流式订阅任务，通过回调处理每个事件
     */
    public void sendSubscribe(String agentUrl, String taskId, String sessionId,
                              String skillId, List<Map<String, Object>> history,
                              Consumer<Map<String, Object>> onEvent,
                              Runnable onComplete) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/sendSubscribe",
                "params", Map.of(
                        "id", taskId,
                        "sessionId", sessionId,
                        "skillId", skillId,
                        "history", history
                ),
                "id", UUID.randomUUID().toString()
        );

        webClient.post()
                .uri(agentUrl + "/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rpcRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        line -> {
                            if (line.startsWith("data:")) {
                                // 解析 SSE data 行，这里简化处理
                                onEvent.accept(Map.of("raw", line.substring(5).trim()));
                            }
                        },
                        error -> log.error("[A2A] SSE 订阅出错: {}", error.getMessage()),
                        onComplete
                );
    }
}