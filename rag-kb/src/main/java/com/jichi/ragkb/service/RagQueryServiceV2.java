package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.DocChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 升级版 RagQueryService：使用混合检索代替纯向量检索。
 * 后续 Reranker 节会在此基础上继续升级。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagQueryServiceV2 {

    private final HybridRetrieverService hybridRetriever;
    private final ChatClient chatClient;

    @Value("${rag.retrieval.return-top-n:5}")
    private int returnTopN;

    public String query(String question, List<Long> kbIds) {
        // Step 1：混合检索
        List<HybridRetrieverService.ScoredChunk> scoredChunks =
                hybridRetriever.retrieve(question, kbIds, returnTopN);

        if (scoredChunks.isEmpty()) {
            return buildNotFoundResponse();
        }

        // Step 2：生成回答
        List<DocChunk> chunks = scoredChunks.stream()
                .map(HybridRetrieverService.ScoredChunk::chunk)
                .collect(Collectors.toList());

        return generateAnswer(question, chunks);
    }

    private String generateAnswer(String question, List<DocChunk> chunks) {
        String context = buildContext(chunks);
        String systemPrompt = buildSystemPrompt(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }

    private String buildContext(List<DocChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocChunk c = chunks.get(i);
            sb.append("[参考").append(i + 1).append("]");
            if (c.getSectionTitle() != null) sb.append(" ").append(c.getSectionTitle());
            sb.append("\n").append(c.getContent()).append("\n\n");
        }
        return sb.toString().strip();
    }

    private String buildSystemPrompt(String context) {
        return """
                你是企业内部知识库的智能助手。根据以下参考内容回答用户问题。
                
                规则：
                1. 只基于参考内容回答，不使用自身知识推测
                2. 参考内容不足时，明确告知"未在知识库找到相关信息"
                3. 回答用中文，准确简洁
                4. 禁止编造参考内容之外的信息
                
                参考内容：
                ---
                %s
                ---
                """.formatted(context);
    }

    private String buildNotFoundResponse() {
        return "在您选择的知识库中未找到与该问题相关的内容。建议您：\n" +
               "1. 确认问题是否与知识库主题相关\n" +
               "2. 尝试用不同关键词提问\n" +
               "3. 联系相关部门获取准确信息";
    }
}