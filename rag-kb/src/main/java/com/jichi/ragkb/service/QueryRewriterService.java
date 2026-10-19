package com.jichi.ragkb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryRewriterService {

    private final ChatClient chatClient;
    private final TokenMetrics tokenMetrics;
    private final ContextTrimmerService contextTrimmer;

    /**
     * HyDE：生成假设性回答，用于向量检索。
     * 缓存：相同问题的假设回答缓存 10 分钟，避免重复调模型。
     */
    @Cacheable(value = "hyde-cache", key = "#question.hashCode()")
    public String generateHypotheticalAnswer(String question) {
        log.debug("[QueryRewriter] HyDE 生成假设回答：{}", question);

        String prompt = """
                请根据以下问题，生成一个简洁的假设性回答（2-4句话）。
                这个回答不需要准确，只需要在语义上覆盖可能的答案内容。
                直接给出答案内容，不要任何前缀说明。
                
                问题：%s
                """.formatted(question);

        try {
            String hypothetical = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.debug("[QueryRewriter] HyDE 结果：{}", hypothetical);

            int genTokens = contextTrimmer.countTokens(hypothetical);
            tokenMetrics.recordGenerationTokens(genTokens);

            return hypothetical;
        } catch (Exception e) {
            // HyDE 失败不影响主流程，降级返回原始问题
            log.warn("[QueryRewriter] HyDE 生成失败，使用原始问题：{}", e.getMessage());
            return question;
        }
    }

    /**
     * 多路查询扩展：把问题扩展成多个角度。
     * 返回的列表包含原始问题 + 扩展问题。
     */
    public List<String> expandQuery(String question) {
        log.debug("[QueryRewriter] 多路扩展：{}", question);

        String prompt = """
                请将以下问题改写成3个不同表达方式的查询，要求：
                1. 保持原始意图不变
                2. 每个查询角度略有不同（换词、换句式、从不同维度提问）
                3. 每行一个查询，不要编号，不要任何额外说明
                
                原始问题：%s
                """.formatted(question);

        try {
            String expanded = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            int genTokens = contextTrimmer.countTokens(expanded);
            tokenMetrics.recordGenerationTokens(genTokens);

            List<String> queries = new ArrayList<>();
            queries.add(question);  // 原始问题也要检索

            Arrays.stream(expanded.split("\n"))
                    .map(String::strip)
                    .filter(q -> !q.isBlank() && !q.equals(question))
                    .limit(3)
                    .forEach(queries::add);

            log.debug("[QueryRewriter] 扩展结果：{}", queries);
            return queries;

        } catch (Exception e) {
            log.warn("[QueryRewriter] 查询扩展失败，使用原始问题：{}", e.getMessage());
            return List.of(question);
        }
    }
}