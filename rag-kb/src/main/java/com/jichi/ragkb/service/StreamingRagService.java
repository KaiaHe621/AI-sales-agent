package com.jichi.ragkb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.ragkb.dto.RagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingRagService {

    private final EnhancedRetrieverService enhancedRetriever;
    private final RerankerService rerankerService;
    private final ConfidenceFilter confidenceFilter;
    private final ContextTrimmerService contextTrimmer;
    private final SourceBuilder sourceBuilder;
    private final ChatSessionService sessionService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final TokenMetrics tokenMetrics;

    /**
     * 流式 RAG 查询，通过 SSE 推送结果。
     */
    public void streamQuery(String question, List<Long> kbIds, String sessionId, SseEmitter emitter) {
        long start = System.currentTimeMillis();

        try {
            // Step 1：推送"检索中"状态
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data("{\"type\":\"RETRIEVING\",\"message\":\"正在检索知识库...\"}"));

            // Step 2：执行检索管道
            var candidates = enhancedRetriever.retrieveWithHyde(question, kbIds, 20);
            var reranked = rerankerService.rerank(question, candidates, 5);
            var filtered = confidenceFilter.filter(reranked);

            if (filtered.isEmpty()) {
                sendNotFound(emitter);
                return;
            }

            var trimmed = contextTrimmer.trim(filtered);

            // Step 3：推送"生成中"状态
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data("{\"type\":\"GENERATING\",\"message\":\"已找到相关内容，正在生成回答...\"}"));

            // Step 4：构建 Prompt
            String context = buildContext(trimmed);
            String systemPrompt = RagPromptTemplate.buildSystemPrompt(context, trimmed.size());

            // Step 5：流式生成，逐 Token 推送
            StringBuilder fullAnswer = new StringBuilder();

            chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        try {
                            fullAnswer.append(token);
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                        } catch (IOException e) {
                            log.warn("[StreamRAG] SSE 推送 Token 失败，客户端可能已断开");
                            throw new RuntimeException("SSE 连接断开");
                        }
                    })
                    .blockLast();  // 等待生成完成

            // Step 6：保存对话记录 + 记录 Generation Token
            String answer = fullAnswer.toString();
            int genTokens = contextTrimmer.countTokens(answer);
            tokenMetrics.recordGenerationTokens(genTokens);

            List<RagResponse.Source> sources = sourceBuilder.buildSources(answer, trimmed);
            String sourcesJson = sourceBuilder.sourcesToJson(sources);
            sessionService.saveMessage(sessionId, question, answer, sourcesJson,
                    (int) (System.currentTimeMillis() - start));

            // Step 7：推送来源信息（done 事件）
            String doneData = objectMapper.writeValueAsString(
                    new DonePayload(sources, (int) (System.currentTimeMillis() - start)));
            emitter.send(SseEmitter.event().name("done").data(doneData));
            emitter.complete();

        } catch (Exception e) {
            log.error("[StreamRAG] 流式生成失败：{}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"生成失败：" + e.getMessage() + "\"}"));
                emitter.complete();
            } catch (IOException ignored) {}
        }
    }

    /**
     * 同步版本（供测试使用）。
     */
    public RagResponse syncQuery(String question, List<Long> kbIds, String sessionId) {
        long start = System.currentTimeMillis();

        var candidates = enhancedRetriever.retrieveWithHyde(question, kbIds, 20);
        var reranked = rerankerService.rerank(question, candidates, 5);
        var filtered = confidenceFilter.filter(reranked);

        if (filtered.isEmpty()) return RagResponse.notFound();

        var trimmed = contextTrimmer.trim(filtered);
        String context = buildContext(trimmed);
        String systemPrompt = RagPromptTemplate.buildSystemPrompt(context, trimmed.size());

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        int genTokens = contextTrimmer.countTokens(answer);
        tokenMetrics.recordGenerationTokens(genTokens);

        List<RagResponse.Source> sources = sourceBuilder.buildSources(answer, trimmed);
        String sourcesJson = sourceBuilder.sourcesToJson(sources);
        int latencyMs = (int) (System.currentTimeMillis() - start);

        sessionService.saveMessage(sessionId, question, answer, sourcesJson, latencyMs);

        return RagResponse.builder()
                .answer(answer)
                .sources(sources)
                .latencyMs(latencyMs)
                .build();
    }

    private String buildContext(List<HybridRetrieverService.ScoredChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            var sc = chunks.get(i);
            sb.append("[参考").append(i + 1).append("]");
            if (sc.chunk().getSectionTitle() != null) sb.append(" ").append(sc.chunk().getSectionTitle());
            sb.append("\n").append(sc.content()).append("\n\n");
        }
        return sb.toString().strip();
    }

    private void sendNotFound(SseEmitter emitter) throws IOException {
        String msg = "在知识库中未找到与该问题相关的内容。请尝试用不同关键词提问，或联系相关部门。";
        emitter.send(SseEmitter.event().name("token").data(msg));
        emitter.send(SseEmitter.event().name("done").data("{\"sources\":[]}"));
        emitter.complete();
    }

    record DonePayload(List<RagResponse.Source> sources, int latencyMs) {}
}