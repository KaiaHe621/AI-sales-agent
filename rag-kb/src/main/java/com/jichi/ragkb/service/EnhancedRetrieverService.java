package com.jichi.ragkb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 带查询改写的检索服务。
 * 策略：原始问题 + HyDE 假设答案 → 各自向量化 → 多路检索 → RRF 融合。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnhancedRetrieverService {

    private final HybridRetrieverService hybridRetriever;
    private final QueryRewriterService queryRewriter;
    private final EmbeddingService embeddingService;
    private final com.jichi.ragkb.repository.DocChunkRepository chunkRepository;

    @Value("${rag.retrieval.vector-top-k:20}")
    private int vectorTopK;

    @Value("${rag.retrieval.fulltext-top-k:20}")
    private int fulltextTopK;

    private static final int RRF_K = 60;

    /**
     * 带 HyDE 的增强检索。
     * 用原始问题 + HyDE 假设答案的向量分别检索，RRF 融合。
     *
     * @param question 用户原始问题
     * @param kbIds    知识库 ID 列表
     * @param topN     返回数量
     */
    public List<HybridRetrieverService.ScoredChunk> retrieveWithHyde(
            String question, List<Long> kbIds, int topN) {

        // 路线1：原始问题的混合检索结果
        List<HybridRetrieverService.ScoredChunk> originalResults =
                hybridRetriever.retrieve(question, kbIds, vectorTopK);

        // 路线2：HyDE 假设答案的向量检索结果
        String hydeAnswer = queryRewriter.generateHypotheticalAnswer(question);
        float[] hydeEmbedding = embeddingService.embed(hydeAnswer);
        String hydeEmbeddingStr = toVectorString(hydeEmbedding);

        List<com.jichi.ragkb.entity.DocChunk> hydeResults = kbIds.stream()
                .flatMap(kbId -> chunkRepository.findByVectorSimilarity(kbId, hydeEmbeddingStr, vectorTopK).stream())
                .collect(Collectors.toList());

        log.debug("[EnhancedRetriever] 原始检索={}，HyDE检索={}", originalResults.size(), hydeResults.size());

        // RRF 融合两路结果
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        Map<Long, com.jichi.ragkb.entity.DocChunk> chunkMap = new HashMap<>();

        // 原始结果按已有 RRF 分数参与融合
        for (int rank = 0; rank < originalResults.size(); rank++) {
            HybridRetrieverService.ScoredChunk sc = originalResults.get(rank);
            double rrfScore = 1.0 / (RRF_K + rank + 1);
            scoreMap.merge(sc.id(), rrfScore, Double::sum);
            chunkMap.put(sc.id(), sc.chunk());
        }

        // HyDE 结果
        for (int rank = 0; rank < hydeResults.size(); rank++) {
            com.jichi.ragkb.entity.DocChunk chunk = hydeResults.get(rank);
            double rrfScore = 1.0 / (RRF_K + rank + 1);
            scoreMap.merge(chunk.getId(), rrfScore, Double::sum);
            chunkMap.put(chunk.getId(), chunk);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .map(e -> new HybridRetrieverService.ScoredChunk(chunkMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}