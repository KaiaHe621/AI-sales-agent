package com.jichi.ragkb.service;

import com.jichi.ragkb.dto.RagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FullRagPipeline {

    private final EnhancedRetrieverService enhancedRetriever;
    private final RerankerService rerankerService;
    private final ConfidenceFilter confidenceFilter;
    private final ContextTrimmerService contextTrimmer;
    private final SourceBuilder sourceBuilder;
    private final HallucinationChecker hallucinationChecker;
    private final ChatClient chatClient;

    @Value("${reranker.top-n:5}")
    private int rerankerTopN;

    public RagResponse query(String question, List<Long> kbIds) {
        long pipelineStart = System.currentTimeMillis();

        // Step 1：增强检索（混合检索 + HyDE）
        List<HybridRetrieverService.ScoredChunk> candidates =
                enhancedRetriever.retrieveWithHyde(question, kbIds, 20);

        if (candidates.isEmpty()) {
            return RagResponse.notFound();
        }

        // Step 2：Reranker 精排
        List<HybridRetrieverService.ScoredChunk> reranked =
                rerankerService.rerank(question, candidates, rerankerTopN);

        // Step 3：置信度过滤
        List<HybridRetrieverService.ScoredChunk> filtered = confidenceFilter.filter(reranked);

        if (filtered.isEmpty()) {
            return RagResponse.notFound();
        }

        // Step 4：上下文裁剪（控制 Token 预算）
        List<HybridRetrieverService.ScoredChunk> trimmed = contextTrimmer.trim(filtered);

        // Step 5：构建带引用编号的 context + 用 RagPromptTemplate 生成 System Prompt
        String context = buildContext(trimmed);
        String answer = generateAnswer(question, context, trimmed.size());

        // Step 6：用 SourceBuilder 解析引用标注，关联到文档信息
        List<RagResponse.Source> sources = sourceBuilder.buildSources(answer, trimmed);

        // Step 7：幻觉检测（抽样，每 5 次查询跑 1 次，避免增加太多成本）
        if (System.currentTimeMillis() % 5 == 0) {
            var faithResult = hallucinationChecker.check(question, answer, context);
            if (!faithResult.isFaithful()) {
                log.warn("[FullRagPipeline] 幻觉检测不通过：score={}，reason={}",
                        faithResult.score(), faithResult.reason());
            }
        }

        long elapsed = System.currentTimeMillis() - pipelineStart;
        log.info("[FullRagPipeline] 完成：question={}，elapsed={}ms，sources={}",
                question.substring(0, Math.min(30, question.length())), elapsed, sources.size());

        return RagResponse.builder()
                .answer(answer)
                .sources(sources)
                .latencyMs((int) elapsed)
                .build();
    }

    private String buildContext(List<HybridRetrieverService.ScoredChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            var sc = chunks.get(i);
            sb.append("[参考").append(i + 1).append("]");
            if (sc.chunk().getSectionTitle() != null) {
                sb.append(" ").append(sc.chunk().getSectionTitle());
            }
            sb.append("\n").append(sc.content()).append("\n\n");
        }
        return sb.toString().strip();
    }

    private String generateAnswer(String question, String context, int chunkCount) {
        String systemPrompt = RagPromptTemplate.buildSystemPrompt(context, chunkCount);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }
}