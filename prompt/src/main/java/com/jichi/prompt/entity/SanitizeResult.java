package com.jichi.prompt.entity;

public record SanitizeResult(boolean blocked, String message, String cleanedInput) {
    public static SanitizeResult ok(String input) {
        return new SanitizeResult(false, null, input);
    }
    public static SanitizeResult blocked(String reason) {
        return new SanitizeResult(true, reason, null);
    }
}