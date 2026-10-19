package com.jichi.ragkb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将用户查询转换为 PostgreSQL tsquery 格式。
 *
 * 简单实现：按空格和常见分隔符切分，提取有意义的词。
 * 生产中可以接入 jieba 等中文分词工具效果更好。
 *
 * 不单独暴露 HTTP，由下面的 HybridRetrieverService 注入使用即可。
 */
@Component
@Slf4j
public class TsQueryBuilder {

    // 停用词（这些词在全文检索中无意义）
    private static final List<String> STOP_WORDS = List.of(
            "的", "了", "是", "在", "有", "和", "与", "或", "这", "那",
            "什么", "怎么", "如何", "为什么", "哪些", "怎样", "请问",
            "a", "an", "the", "is", "are", "what", "how"
    );

    /**
     * 将问题转为 tsquery 格式。
     * 例如："API 限流策略是什么" → "API & 限流 & 策略"
     */
    public String build(String query) {
        if (query == null || query.isBlank()) return null;

        // 按空格、标点切分
        String[] tokens = query.split("[\\s\\p{P}]+");

        List<String> keywords = Arrays.stream(tokens)
                .map(String::strip)
                .filter(t -> !t.isBlank())
                .filter(t -> t.length() >= 2)              // 过滤单字符
                .filter(t -> !STOP_WORDS.contains(t.toLowerCase()))
                .collect(Collectors.toList());

        if (keywords.isEmpty()) {
            // 降级：取整个查询的前20字符
            keywords = List.of(query.substring(0, Math.min(20, query.length())));
        }

        // 用 & 连接（AND 查询），至少含所有关键词
        String tsQuery = String.join(" & ", keywords);
        log.debug("[TsQuery] query='{}' → tsQuery='{}'", query, tsQuery);
        return tsQuery;
    }
}