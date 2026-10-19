package com.jichi.prompt.entity;

import java.util.List;

public record ReviewAnalysis(
    String sentiment,           // POSITIVE / NEGATIVE / MIXED / NEUTRAL
    List<String> positiveAspects,
    List<String> negativeAspects,
    int overallScore,           // 1-10
    String summary
) {}