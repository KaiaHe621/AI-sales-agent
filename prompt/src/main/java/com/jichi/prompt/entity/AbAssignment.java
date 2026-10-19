package com.jichi.prompt.entity;

public record AbAssignment(
    String experimentId,
    String userId,
    String variant,        // A 或 B
    String promptContent   // 分配到的 Prompt 内容
) {}