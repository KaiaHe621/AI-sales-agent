package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.ProductEntity;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVectorService {

    private final JdbcTemplate jdbc;
    private final EmbeddingService embeddingService;
    private final ProductTextBuilder textBuilder;

    /**
     * 单条商品写入向量。embedding 列为空或过期都靠这个方法刷新。
     */
    public void upsertEmbedding(ProductEntity p) {
        float[] vec = embeddingService.embed(textBuilder.build(p));
        jdbc.update(
                "UPDATE product SET embedding = ? WHERE id = ?",
                new PGvector(vec), p.getId());
    }

    /**
     * 向量 + 标量混合检索。
     *
     * @param query       用户 query（会被向量化）
     * @param extraFilter 额外 WHERE 条件片段（不含 WHERE 关键字），可为空
     * @param extraParams extraFilter 里的 ? 对应的参数
     * @param topK        返回前 K 个
     * @return 商品 id 列表，按相似度降序
     */
    public List<Long> search(String query, String extraFilter, List<Object> extraParams, int topK) {
        float[] qvec = embeddingService.embed(query);

        StringBuilder sql = new StringBuilder(
                "SELECT id FROM product " +
                        "WHERE status = 'ON_SALE' AND embedding IS NOT NULL");
        if (extraFilter != null && !extraFilter.isBlank()) {
            sql.append(" AND ").append(extraFilter);
        }
        // <=> 是 pgvector 的 cosine distance 运算符，越小越相似
        sql.append(" ORDER BY embedding <=> ? LIMIT ?");

        Object[] params = new Object[extraParams.size() + 2];
        for (int i = 0; i < extraParams.size(); i++) params[i] = extraParams.get(i);
        params[extraParams.size()] = new PGvector(qvec);
        params[extraParams.size() + 1] = topK;

        return jdbc.query(sql.toString(), params,
                (rs, i) -> rs.getLong("id"));
    }
}