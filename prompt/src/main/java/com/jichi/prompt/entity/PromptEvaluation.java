package com.jichi.prompt.entity;

import java.util.List;
import java.util.Map;

public record PromptEvaluation(
    Map<String, Integer> scores,
    int overallScore,
    List<String> strengths,
    List<String> improvements
) {}