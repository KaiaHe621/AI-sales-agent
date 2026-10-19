package com.jichi.prompt.entity;

import java.util.List;

public record CodeReviewResult(
        String reviewedCode,
        List<String> fixedIssues,
        List<String> suggestions
) {
}