package com.jichi.voiceshopping.memory;

import com.jichi.voiceshopping.dto.Intent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TurnSummarizer {

    /**
     * 把一轮用户原话 + Agent 回复 + 意图 summary，压成一句话。
     * 作为"最近历史"的表达方式。
     */
    public String summarize(String userUtterance, Intent intent, String agentReply) {
        return String.format("[%s] 用户：%s / 助手：%s",
                intent.name(),
                truncate(userUtterance, 30),
                truncate(agentReply, 40));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}