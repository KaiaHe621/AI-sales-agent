package com.jichi.a2a.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

// Part（多态）
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Part(
        String type,   // "text", "data", "file"
        String text,
        Map<String, Object> data
) {
    public static Part text(String text) {
        return new Part("text", text, null);
    }

    public static Part data(Map<String, Object> data) {
        return new Part("data", null, data);
    }
}