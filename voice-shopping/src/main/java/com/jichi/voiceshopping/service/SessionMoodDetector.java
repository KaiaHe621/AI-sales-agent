package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.memory.ShortTermMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SessionMoodDetector {

    private final ShortTermMemory memory;

    /** 返回：neutral / hesitant / impatient / positive / negative */
    public String detect(String sessionId, String currentUtterance) {
        // 先走规则，覆盖 80% 场景
        String rule = byRule(currentUtterance);
        if (rule != null) return rule;

        // 再看最近几轮是不是在反复追问（犹豫）
        var recent = memory.recent(sessionId, 4);
        long askTurns = recent.stream().filter(t -> "ASSISTANT".equals(t.role())
                && t.text() != null && t.text().endsWith("？")).count();
        if (askTurns >= 2) return "hesitant";

        return "neutral";
    }

    private static final Pattern IMPATIENT = Pattern.compile("(算了|不要|别说了|快点|到底|跳过)");
    private static final Pattern POSITIVE = Pattern.compile("(好的|可以|不错|挺好|行|ok)");
    private static final Pattern NEGATIVE = Pattern.compile("(贵|太贵|不喜欢|丑|不行|不对)");

    private String byRule(String text) {
        if (text == null) return null;
        if (IMPATIENT.matcher(text).find()) return "impatient";
        if (NEGATIVE.matcher(text).find()) return "negative";
        if (POSITIVE.matcher(text).find()) return "positive";
        return null;
    }
}