package com.jichi.ragkb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析模型回答中的引用标注，关联到具体 chunk。
 */
@Component
@Slf4j
public class CitationParser {

    // 匹配 (来源：[参考1][参考2]) 或 [参考1] 等格式
    private static final Pattern CITATION_PATTERN =
            Pattern.compile("(?:（来源：|\\[)参考(\\d+)(?:）|\\])");

    /**
     * 从回答中提取引用，返回被引用的 chunk 索引（1-based）。
     */
    public Set<Integer> extractCitedIndices(String answer) {
        Set<Integer> cited = new LinkedHashSet<>();
        Matcher m = CITATION_PATTERN.matcher(answer);
        while (m.find()) {
            try {
                cited.add(Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignored) {}
        }
        return cited;
    }

    /**
     * 清理回答文本中的引用标记（前端展示时可能需要纯净文本）。
     */
    public String cleanCitations(String answer) {
        return answer
                .replaceAll("（来源：(?:\\[参考\\d+\\])+）", "")
                .replaceAll("\\[参考\\d+\\]", "")
                .replaceAll("\\s+", " ")
                .strip();
    }
}