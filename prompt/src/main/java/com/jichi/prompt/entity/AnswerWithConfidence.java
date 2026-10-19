package com.jichi.prompt.entity;

public record AnswerWithConfidence(String answer, double confidence, int sampleCount) {}
