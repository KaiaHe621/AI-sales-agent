package com.jichi.prompt.entity;

public record VariantStats(
    String variant,
    long totalRequests,
    double successRate,
    double avgRating,
    double ratingStdDev
) {}