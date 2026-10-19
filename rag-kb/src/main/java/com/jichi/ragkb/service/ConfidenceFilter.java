package com.jichi.ragkb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 置信度过滤器：过滤掉低分 chunk，避免不相关内容影响生成质量。
 * 由 FullRagPipeline 注入使用，不单独暴露 HTTP。
 */
@Component
@Slf4j
public class ConfidenceFilter {

    @Value("${rag.retrieval.min-score:0.3}")
    private double minScore;

    /**
     * 过滤低置信度的 chunk。
     * 如果过滤后为空，保留分数最高的 1 个（不能完全没有内容）。
     */
    public List<HybridRetrieverService.ScoredChunk> filter(
            List<HybridRetrieverService.ScoredChunk> chunks) {

        List<HybridRetrieverService.ScoredChunk> filtered = chunks.stream()
                .filter(c -> c.score() >= minScore)
                .collect(Collectors.toList());

        if (filtered.isEmpty() && !chunks.isEmpty()) {
            // 至少保留分数最高的 1 个（上游已排序，但以防万一用 max 取最高分）
            HybridRetrieverService.ScoredChunk best = chunks.stream()
                    .max(Comparator.comparingDouble(HybridRetrieverService.ScoredChunk::score))
                    .orElse(chunks.get(0));
            log.debug("[ConfidenceFilter] 所有 chunk 低于阈值 {}，保留最高分1条（score={}）",
                    minScore, best.score());
            filtered = List.of(best);
        }

        int filteredCount = chunks.size() - filtered.size();
        if (filteredCount > 0) {
            log.debug("[ConfidenceFilter] 过滤低置信度 chunk：{}条", filteredCount);
        }

        return filtered;
    }
}