package com.jichi.prompt.entity;

import java.time.LocalDateTime;

public record PromptVersionInfo(
        String version,
        String status,
        String description,
        String createdBy,
        LocalDateTime createdAt
) {
}