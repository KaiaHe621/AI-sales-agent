package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.FaqEntryEntity;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqVectorService {

    private final JdbcTemplate jdbc;
    private final EmbeddingService embedding;

    public void upsertEmbedding(FaqEntryEntity f) {
        float[] vec = embedding.embed(f.getQuestion());
        jdbc.update("UPDATE faq_entry SET embedding = ? WHERE id = ?",
                new PGvector(vec), f.getId());
    }

    /**
     * 查询最相似的 FAQ。
     *
     * @param question   用户问题
     * @param merchantId 当前商家 id（会一起查平台通用 0）
     * @param threshold  相似度阈值（范围 0~1，推荐 0.80 起；越低误召回越多）
     * @return FAQ id，未命中则空
     */
    public Optional<Long> searchBest(String question, Long merchantId, double threshold) {
        float[] qvec = embedding.embed(question);
        // pgvector 的 <=> 是 cosine distance = 1 - cosine_similarity。
        // 对归一化的文本 embedding，distance ∈ [0, 1]，相似度 = 1 - distance。
        // 所以阈值换算就是一条直线：maxDistance = 1 - threshold。
        double maxDistance = 1.0 - threshold;

        List<Object[]> rows = jdbc.query(
                "SELECT id, embedding <=> ? AS dist " +
                "FROM faq_entry " +
                "WHERE status = 'PUBLISHED' " +
                "  AND merchant_id IN (0, ?) " +
                "  AND embedding IS NOT NULL " +
                "ORDER BY dist ASC LIMIT 1",
                (rs, i) -> new Object[]{ rs.getLong("id"), rs.getDouble("dist") },
                new PGvector(qvec), merchantId);

        if (rows.isEmpty()) return Optional.empty();
        Object[] top = rows.get(0);
        long id = (long) top[0];
        double dist = (double) top[1];
        double similarity = 1.0 - dist;
        log.debug("FAQ 检索 top1: id={}, similarity={}, threshold={}", id, similarity, threshold);

        return similarity >= threshold ? Optional.of(id) : Optional.empty();
    }
}