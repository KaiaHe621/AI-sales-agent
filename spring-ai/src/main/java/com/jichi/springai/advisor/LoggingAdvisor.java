package com.jichi.springai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();

        // 打印请求（getContents() 返回所有消息拼接后的文本）
        String userMessage = request.prompt().getContents();
        log.info("[AI调用] 用户消息: {}", userMessage);

        // 继续执行链（调用下一个 Advisor 或最终调用模型）
        ChatClientResponse response = chain.nextCall(request);

        // 打印响应
        long elapsed = System.currentTimeMillis() - start;
        String aiReply = response.chatResponse().getResult().getOutput().getText();
        log.info("[AI调用] 模型回复（{}ms）: {}", elapsed,
                aiReply.length() > 100 ? aiReply.substring(0, 100) + "..." : aiReply);

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.currentTimeMillis();
        log.info("[AI流式调用] 用户消息: {}", request.prompt().getContents());

        return chain.nextStream(request)
                .doOnComplete(() -> log.info("[AI流式调用] 完成，耗时 {}ms",
                        System.currentTimeMillis() - start));
    }

    @Override
    public String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 最先执行，保证记录完整耗时
    }
}