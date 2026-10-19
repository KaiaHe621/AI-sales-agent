package com.jichi.voiceshopping.compliance;

import com.jichi.voiceshopping.dto.EmotionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceChecker {

    @Value("${voice-shopping.compliance.absolute-claim-patterns}")
    private List<String> absolutePatterns;

    private Set<String> sensitiveWords = new HashSet<>();

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ClassPathResource("compliance/sensitive-words.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) sensitiveWords.add(line);
            }
        }
    }

    public EmotionResult ensureCompliant(String sessionId, Long userId, EmotionResult input) {
        if (input == null || input.speechText() == null) return input;

        String text = input.speechText();
        String cleaned = text;

        // 绝对化用词替换
        for (String abs : absolutePatterns) {
            cleaned = cleaned.replace(abs, soften(abs));
        }
        // 敏感词直接过滤为 *
        for (String w : sensitiveWords) {
            if (cleaned.contains(w)) cleaned = cleaned.replace(w, "*".repeat(w.length()));
        }

        if (!cleaned.equals(text)) {
            log.warn("[Compliance] session={} 触发合规改写", sessionId);
        }

        return new EmotionResult(cleaned, input.displayBlocks());
    }

    private String soften(String abs) {
        return switch (abs) {
            case "最好" -> "比较合适";
            case "第一" -> "常用";
            case "保证" -> "通常";
            case "绝对" -> "基本";
            default -> abs;
        };
    }
}