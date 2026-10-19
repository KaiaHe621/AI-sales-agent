package com.jichi.prompt.entity;

public record ValidatedResult(
    ContactInfo data,
    ValidationResult validation
) {}