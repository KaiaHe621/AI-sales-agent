package com.jichi.langchain4j.model;

public record CodeReviewRequest(
    String language,
    String code,
    String focusAreas  // "性能、空指针、事务"
) {}