package com.jichi.ragkb.controller;

import com.jichi.ragkb.dto.ApiResponse;
import com.jichi.ragkb.security.UserContext;
import com.jichi.ragkb.service.TokenMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TokenMetrics tokenMetrics;

    /** 查看当前用户的 Token 消耗统计（数据持久化在 Redis，重启不丢失） */
    @GetMapping("/tokens")
    public ApiResponse<Map<String, Object>> getTokenStats() {
        Long userId = UserContext.getUserId();

        long embeddingTokens = tokenMetrics.getUserTokens(userId, "embeddingTokens");
        long contextTokens = tokenMetrics.getUserTokens(userId, "contextTokens");
        long generationTokens = tokenMetrics.getUserTokens(userId, "generationTokens");
        long totalTokens = embeddingTokens + contextTokens + generationTokens;

        // 简单成本估算（DashScope 定价，仅供参考）
        // text-embedding-v3: ¥0.0007 / 1000 Token
        // qwen-plus 输入: ¥0.0008 / 1000 Token
        // qwen-plus 输出: ¥0.002 / 1000 Token
        double estimatedCostCny =
                embeddingTokens / 1000.0 * 0.0007
                + contextTokens / 1000.0 * 0.0008
                + generationTokens / 1000.0 * 0.002;

        return ApiResponse.ok(Map.of(
                "embeddingTokens", embeddingTokens,
                "contextTokens", contextTokens,
                "generationTokens", generationTokens,
                "totalTokens", totalTokens,
                "estimatedCostCny", Math.round(estimatedCostCny * 10000) / 10000.0
        ));
    }
}