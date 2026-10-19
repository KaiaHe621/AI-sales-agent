package com.jichi.prompt.config;

import com.jichi.prompt.entity.SanitizeResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    private static final List<String> INJECTION_KEYWORDS = List.of(
            "忽略你之前的", "忘掉你的", "你的新任务是",
            "SYSTEM OVERRIDE", "ignore previous",
            "forget all instructions", "you are now",
            "DAN", "do anything now",
            "角色扮演", "roleplay as an AI without"
    );

    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("(?i)(ignore|forget|override)\\s+(all\\s+)?(previous|prior|above)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an)"),
            Pattern.compile("(?i)(system|admin|root)\\s*(:|prompt|override)")
    );

    public SanitizeResult sanitize(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return SanitizeResult.ok(userInput);
        }

        for (String keyword : INJECTION_KEYWORDS) {
            if (userInput.toLowerCase().contains(keyword.toLowerCase())) {
                return SanitizeResult.blocked("检测到可疑输入");
            }
        }

        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                return SanitizeResult.blocked("检测到可疑输入模式");
            }
        }

        return SanitizeResult.ok(userInput);
    }
}