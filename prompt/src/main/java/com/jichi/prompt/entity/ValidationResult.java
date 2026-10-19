package com.jichi.prompt.entity;

import java.util.List;

public record ValidationResult(
    boolean valid,
    List<String> errors,
    List<String> warnings
) {}