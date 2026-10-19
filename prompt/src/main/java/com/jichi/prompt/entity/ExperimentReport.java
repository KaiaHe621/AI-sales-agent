package com.jichi.prompt.entity;

public record ExperimentReport(
    String experimentId,
    VariantStats variantA,
    VariantStats variantB,
    String winner,      // A / B / INCONCLUSIVE
    String conclusion
) {}