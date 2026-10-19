package com.jichi.voiceshopping.dto;

import java.util.Map;

/**
 * Intent Agent 的结构化输出。
 *
 * @param intent     分类到的意图
 * @param slots      从用户话里抽到的槽位（category/budget/scenario 等），抽不到的填 null
 * @param confidence 模型给出的置信度 [0.0, 1.0]，JSON 解析失败降级时会填 0.3
 */
public record IntentResult(
        Intent intent,
        Map<String, Object> slots,
        double confidence
) {}