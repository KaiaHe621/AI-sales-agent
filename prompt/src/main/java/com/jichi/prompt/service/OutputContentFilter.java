package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.FilterResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class OutputContentFilter {

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(?i)步骤\\s*[一1].*步骤\\s*[二2].*制作|合成|爆炸"),
            Pattern.compile("(?i)(配方|原料|材料).*[危险|爆炸|炸弹|毒]"),
            Pattern.compile("(?i)社会工程.*诈骗.*话术")
    );

    private final ChatClient moderationClient;

    public OutputContentFilter(DashScopeChatModel chatModel) {
        this.moderationClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是内容安全审核员。
                        判断以下 AI 回复是否包含有害内容（武器制造、诈骗话术、歧视性内容等）。
                        只输出 SAFE 或 UNSAFE。
                        """)
                .build();
    }

    public FilterResult filter(String content) {
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return new FilterResult(false, "规则过滤触发");
            }
        }

        // AI 深度审核（可选，有额外调用成本）
        // String verdict = moderationClient.prompt()
        //         .user(content).call().content().trim();
        // if ("UNSAFE".equals(verdict)) return new FilterResult(false, "AI审核触发");

        return new FilterResult(true, null);
    }
}