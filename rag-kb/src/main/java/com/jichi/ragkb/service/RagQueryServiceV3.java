package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.DocChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagQueryServiceV3 {

    private final EnhancedRetrieverService enhancedRetriever;
    private final ChatClient chatClient;

    public String query(String question, List<Long> kbIds) {
        List<HybridRetrieverService.ScoredChunk> scoredChunks =
                enhancedRetriever.retrieveWithHyde(question, kbIds, 5);

        if (scoredChunks.isEmpty()) {
            return "在您选择的知识库中未找到与该问题相关的内容。";
        }

        List<DocChunk> chunks = scoredChunks.stream()
                .map(HybridRetrieverService.ScoredChunk::chunk)
                .collect(Collectors.toList());

        return generateAnswer(question, chunks);
    }

    private String generateAnswer(String question, List<DocChunk> chunks) {
        String context = buildContext(chunks);

        String systemPrompt = """
                你是企业内部知识库的智能助手。根据提供的参考内容回答问题。
                规则：只根据参考内容回答，不要编造；如果参考内容不够，告诉用户未找到相关信息。
                
                参考内容：
                ---
                %s
                ---
                """.formatted(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
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
}