package com.jichi.voiceshopping.dto;

import java.util.List;

/**
 * 推荐 Agent 的最终输出，交给情感 Agent 做口语化包装。
 *
 * @param items           经过加权 + 理由生成后的 Top N 候选
 * @param explanationTone 提示情感 Agent 使用哪种口吻（professional / casual / comfort…）
 */
public record RecommendResult(
        List<RecommendedItem> items,
        String explanationTone
) {}