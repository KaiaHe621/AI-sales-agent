package com.jichi.a2a.server.model;

import java.util.List;

// Message
public record Message(
        String role,   // "user" 或 "agent"
        List<Part> parts
) {
}