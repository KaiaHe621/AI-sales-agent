package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.DocChunk;
import com.jichi.ragkb.repository.DocChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagQueryService {

    private final EmbeddingService embeddingService;
    private final DocChunkRepository chunkRepository;
    private final ChatClient chatClient;

    @Value("${rag.retrieval.vector-top-k:5}")
    private int vectorTopK;

    @Value("${rag.retrieval.min-score:0.5}")
    private double minScore;

    /**
     * 基础 RAG 查询：向量检索 + 生成回答。
     *
     * @param question 用户问题
     * @param kbIds    要查询的知识库 ID 列表
     * @return 生成的答案
     */
    public String query(String question, List<Long> kbIds) {
        // Step 1：向量化问题
        float[] queryEmbedding = embeddingService.embed(question);

        // Step 2：向量检索（从多个知识库）
        List<DocChunk> retrievedChunks = retrieveChunks(queryEmbedding, kbIds, vectorTopK);

        if (retrievedChunks.isEmpty()) {
            return "在您选择的知识库中未找到与该问题相关的内容。请确认问题是否与知识库的主题相关，或尝试用不同的表达方式提问。";
        }

        // Step 3：组装 Prompt 并生成回答
        return generateAnswer(question, retrievedChunks);
    }

    /**
     * 从多个知识库执行向量检索，合并结果并按相似度排序。
     */
    protected List<DocChunk> retrieveChunks(float[] queryEmbedding, List<Long> kbIds, int topK) {
        // PGVector 格式：[0.1,0.2,...] 字符串
        String embeddingStr = toVectorString(queryEmbedding);

        List<DocChunk> allChunks = kbIds.stream()
                .flatMap(kbId -> chunkRepository.findByVectorSimilarity(kbId, embeddingStr, topK).stream())
                .collect(Collectors.toList());

        // 如果查多个知识库，需要合并后重新排序（近似处理：按 embedding 相似度重排）
        // 注意：此处 chunk 里没有 score 字段，因为 JPA 映射复杂，简化处理：
        // 直接保留 topK 个（后续 Reranker 节会处理精确排序）
        if (allChunks.size() > topK) {
            allChunks = allChunks.subList(0, topK);
        }

        log.debug("[RAG] 向量检索完成：kbIds={}，召回{}条", kbIds, allChunks.size());
        return allChunks;
    }

    /**
     * 组装 System Prompt + Context，调用模型生成答案。
     */
    protected String generateAnswer(String question, List<DocChunk> chunks) {
        String context = buildContext(chunks);

        String systemPrompt = """
                你是企业内部知识库的智能助手。你的工作是根据提供的参考文档内容，准确回答员工的问题。
                
                重要规则：
                1. 只根据提供的【参考内容】回答问题，不要使用自己的知识进行推测或补充
                2. 如果参考内容不足以回答问题，明确告诉用户"在知识库中未找到相关信息"，并建议用户联系相关部门
                3. 回答要准确、简洁，用中文回答
                4. 如果参考内容涉及多个文档，综合各文档回答
                5. 禁止编造不在参考内容中的信息
                
                参考内容如下：
                ---
                %s
                ---
                """.formatted(context);

        log.debug("[RAG] 开始生成回答，context长度={}", context.length());
        long start = System.currentTimeMillis();

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        log.info("[RAG] 生成完成，耗时={}ms，answer长度={}", System.currentTimeMillis() - start, answer.length());
        return answer;
    }

    private String buildContext(List<DocChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocChunk chunk = chunks.get(i);
            sb.append(String.format("[参考%d]", i + 1));
            if (chunk.getSectionTitle() != null) {
                sb.append(" ").append(chunk.getSectionTitle());
            }
            sb.append("\n").append(chunk.getContent()).append("\n\n");
        }
        return sb.toString().strip();
    }

    protected String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}