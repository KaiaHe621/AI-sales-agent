package com.jichi.a2a.server.model;

import java.util.List;
import java.util.Map;

// Task 完整结构
public record Task(
        String id,
        String sessionId,
        TaskStatus status,
        List<Message> history,
        List<Artifact> artifacts,
        Map<String, Object> metadata
) {
}
