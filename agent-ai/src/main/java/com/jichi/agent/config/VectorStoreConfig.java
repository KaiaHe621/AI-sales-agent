package com.jichi.agent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class VectorStoreConfig {

    /**
     * 本地开发 / 测试环境：内存版向量库，重启数据消失，够用来验证逻辑
     * 生产环境注释掉这个 Bean，换成 PGVector 或 Qdrant 的配置
     */
    @Bean
    @Profile("!prod")
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}