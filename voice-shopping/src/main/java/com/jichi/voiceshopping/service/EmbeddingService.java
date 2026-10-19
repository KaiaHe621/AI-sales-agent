package com.jichi.voiceshopping.service;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Value("${voice-shopping.embedding.model}")
    private String model;

    private final TextEmbedding embedding = new TextEmbedding();

    /** 返回 float[]，pgvector Java 客户端要的就是这种类型。 */
    @Cacheable(value = "embed",
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(#text.getBytes())")
    public float[] embed(String text) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .text(text)
                    .build();
            TextEmbeddingResult result = embedding.call(param);
            List<Double> raw = result.getOutput().getEmbeddings().get(0).getEmbedding();
            float[] out = new float[raw.size()];
            for (int i = 0; i < raw.size(); i++) out[i] = raw.get(i).floatValue();
            return out;
        } catch (Exception e) {
            log.error("Embedding 失败：{}", e.getMessage(), e);
            throw new RuntimeException("Embedding 调用失败", e);
        }
    }
}