package com.jichi.prompt.entity;

import com.jichi.prompt.enums.Verdict;

public record ContractRisk(
    Verdict hasRisk,
    String riskType,
    int severity   // 1-10
) {}