package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.RecommendedItem;
import com.jichi.voiceshopping.dto.UserProfileSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProfileReranker {

    public List<RecommendedItem> rerank(List<RecommendedItem> candidates,
                                        UserProfileSnapshot profile,
                                        Map<String, Object> slots) {
        // profile 为空时也要走 budget 锚点打分，不能直接 return
        return candidates.stream()
                .map(it -> it.withMatchScore(computeScore(it, profile, slots)))
                .sorted(Comparator.comparingDouble(RecommendedItem::matchScore).reversed())
                .collect(Collectors.toList());
    }

    private double computeScore(RecommendedItem item, UserProfileSnapshot p, Map<String, Object> slots) {
        double score = item.matchScore();

        // ===== budget 锚点加分：用户给了预算上限时，价格越接近上限（60%~95% 档）的越优先 =====
        // 这条的目的是——用户说"预算 2000"时，客单心锚在 2000 附近，不是奔着 500 去的；
        // 推荐应该倾向接近 budget 的中高端，而不是一来就把最低价顶上来。
        Object budgetObj = slots == null ? null : slots.get("budget");
        if (budgetObj instanceof Number bn && item.price() != null) {
            double budget = bn.doubleValue();
            double price = item.price().doubleValue();
            if (price <= budget && budget > 0) {
                double ratio = price / budget;
                if (ratio >= 0.6)       score += 0.25;    // 主推档，最加分
                else if (ratio >= 0.4)  score += 0.05;    // 中等档，略加分
                else                    score -= 0.10;    // 远低于预算，轻微扣分（可能档次不够）
            }
        }

        if (p != null) {
            // 偏好品牌加分
            String brand = Objects.toString(item.attributes().get("brand"), "");
            Double brandAff = p.brandAffinity() == null ? 0 : p.brandAffinity().getOrDefault(brand, 0.0);
            score += brandAff * 0.2;

            // 价格敏感：超过用户平均客单价太多扣分
            if (p.avgOrderAmount() != null && item.price() != null) {
                double ratio = item.price().doubleValue() / p.avgOrderAmount().doubleValue();
                if (ratio > 1.5) {
                    double sensitivity = p.priceSensitivity() == null ? 0.5 : p.priceSensitivity().doubleValue();
                    score -= 0.15 * sensitivity;
                }
            }

            // 最近已经买过同款/同类扣分
            if (p.recentPurchased() != null && p.recentPurchased().contains(item.productId())) {
                score -= 0.3;
            }
        }

        return score;
    }
}