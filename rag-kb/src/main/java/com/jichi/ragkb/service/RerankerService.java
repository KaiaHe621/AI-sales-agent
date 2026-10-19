package com.jichi.ragkb.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/** 由完整 RAG 管道调用，不单独暴露 HTTP。 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RerankerService {

    private final WebClient.Builder webClientBuilder;
    private final TokenMetrics tokenMetrics;

    @Value("${reranker.endpoint}")
    private String endpoint;

    @Value("${reranker.api-key}")
    private String apiKey;

    @Value("${reranker.model:gte-rerank}")
    private String model;

    @Value("${reranker.timeout-ms:800}")
    private long timeoutMs;

    @Value("${reranker.top-n:5}")
    private int defaultTopN;

    /**
     * 对候选 chunk 进行精排，返回按相关性分数排序的结果。
     * 超时或 API 失败时自动降级（使用 RRF 分数排序）。
     *
     * @param question      用户问题
     * @param candidates    候选 chunk（混合检索结果）
     * @param topN          精排后保留数量
     * @return 精排后的 ScoredChunk 列表
     */
    public List<HybridRetrieverService.ScoredChunk> rerank(
            String question,
            List<HybridRetrieverService.ScoredChunk> candidates,
            int topN) {

        if (candidates.isEmpty()) return candidates;
        if (candidates.size() <= topN) {
            // 候选数量已经不多，不需要精排
            return candidates;
        }

        try {
            List<HybridRetrieverService.ScoredChunk> reranked =
                    callRerankApi(question, candidates, topN);
            log.info("[Reranker] 精排完成：候选={}，返回={}", candidates.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.warn("[Reranker] 精排失败或超时，降级使用 RRF 分数：{}", e.getMessage());
            // 降级：直接用 RRF 分数取 TopN
            return candidates.stream()
                    .limit(topN)
                    .collect(Collectors.toList());
        }
    }

    private List<HybridRetrieverService.ScoredChunk> callRerankApi(
            String question,
            List<HybridRetrieverService.ScoredChunk> candidates,
            int topN) {

        // 构建请求体（DashScope gte-rerank-v2 要求嵌套格式）
        List<String> docs = candidates.stream()
                .map(HybridRetrieverService.ScoredChunk::content)
                .collect(Collectors.toList());

        RerankRequest request = new RerankRequest();
        request.setModel(model);
        request.setInput(new RerankInput(question, docs));
        request.setParameters(new RerankParams(topN, false));

        WebClient client = webClientBuilder
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        RerankResponse response = client.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RerankResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))  // 超时直接走降级
                .block();

        if (response == null || response.getOutput() == null || response.getOutput().getResults() == null) {
            throw new RuntimeException("Reranker API 返回空结果");
        }

        if (response.getUsage() != null && response.getUsage().getTotalTokens() > 0) {
            tokenMetrics.recordContextTokens(response.getUsage().getTotalTokens());
        }

        // 按精排分数组装结果
        return response.getOutput().getResults().stream()
                .sorted(Comparator.comparingDouble(RerankResult::getRelevanceScore).reversed())
                .map(r -> {
                    HybridRetrieverService.ScoredChunk original = candidates.get(r.getIndex());
                    return new HybridRetrieverService.ScoredChunk(
                            original.chunk(),
                            r.getRelevanceScore()
                    );
                })
                .collect(Collectors.toList());
    }

    // =================== DTO ===================

    @Data
    static class RerankRequest {
        private String model;
        private RerankInput input;
        private RerankParams parameters;
    }

    @Data
    static class RerankInput {
        private String query;
        private List<String> documents;

        RerankInput(String query, List<String> documents) {
            this.query = query;
            this.documents = documents;
        }
    }

    @Data
    static class RerankParams {
        @JsonProperty("top_n")
        private int topN;
        @JsonProperty("return_documents")
        private boolean returnDocuments;

        RerankParams(int topN, boolean returnDocuments) {
            this.topN = topN;
            this.returnDocuments = returnDocuments;
        }
    }

    @Data
    static class RerankResponse {
        private RerankOutput output;
        private RerankUsage usage;
    }

    @Data
    static class RerankOutput {
        private List<RerankResult> results;
    }

    @Data
    static class RerankResult {
        private int index;                       // 对应 candidates 列表中的下标
        @JsonProperty("relevance_score")
        private double relevanceScore;           // 精排分数 [0, 1]
    }

    @Data
    static class RerankUsage {
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}