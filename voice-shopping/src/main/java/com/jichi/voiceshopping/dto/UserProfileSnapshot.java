package com.jichi.voiceshopping.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UserProfileSnapshot(
        Long userId,
        String gender,
        Integer age,
        Integer heightCm,
        Integer weightKg,
        String skinType,
        String budgetBand,
        Map<String, Double> categoryAffinity,   // 归一化偏好
        Map<String, Double> brandAffinity,
        List<Long> recentViewed,
        List<Long> recentPurchased,
        BigDecimal priceSensitivity,
        BigDecimal avgOrderAmount
) {
    /**
     * 生成一段适合塞进 Prompt 的自然语言
     */
    public String toNaturalLanguage() {
        StringBuilder sb = new StringBuilder();
        if (gender != null) sb.append("性别").append(gender).append("，");
        if (age != null) sb.append(age).append("岁，");
        if (heightCm != null && weightKg != null)
            sb.append("身高").append(heightCm).append("cm，体重").append(weightKg).append("kg，");
        if (budgetBand != null) sb.append("消费区间").append(budgetBand).append("，");
        if (categoryAffinity != null && !categoryAffinity.isEmpty()) {
            String topCats = categoryAffinity.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .map(e -> e.getKey() + "(" + String.format("%.2f", e.getValue()) + ")")
                    .reduce((a, b) -> a + "，" + b).orElse("");
            sb.append("偏好品类：").append(topCats).append("。");
        }
        return sb.toString();
    }
}