package com.jichi.salesAgent.controller;

public record ChatResponse(
        String sessionId,
        String reply,
        long durationMs
) {}
