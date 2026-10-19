package com.jichi.agentscope.model;

public record ApprovalItem(
        String approvalId,
        String agentName,
        String toolName,
        String toolInput,
        long   createdAt
) {}