package com.jichi.a2a.server.model;

public record TaskStatus(
        TaskState state,
        Message message,
        String timestamp
) {
}
