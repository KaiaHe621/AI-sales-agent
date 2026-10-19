package com.jichi.prompt.entity;

public record EvaluationResult(
    int accuracy,
    int relevance,
    int safety,
    int userExperience,
    String notes
) {}