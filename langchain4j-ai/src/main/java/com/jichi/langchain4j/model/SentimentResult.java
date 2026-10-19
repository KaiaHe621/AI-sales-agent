package com.jichi.langchain4j.model;

import java.util.List;

public record SentimentResult(
        String sentiment,       // POSITIVE / NEGATIVE / MIXED / NEUTRAL
        List<String> reasons,   // 判断依据
        int score               // 1-10 分
) {}