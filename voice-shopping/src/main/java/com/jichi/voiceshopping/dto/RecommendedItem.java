package com.jichi.voiceshopping.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 一条推荐候选：候选生成、画像加权、理由生成、最终出口，自始至终传的都是这个 record。
 *
 * @param productId   商品 ID
 * @param name        商品名
 * @param price       价格
 * @param reason      推荐理由（候选阶段为空，理由生成阶段回填）
 * @param matchScore  匹配分，候选阶段是向量相似度，加权后是综合分
 * @param attributes  扩展属性（category_l2、brand、颜色、适用场景等）
 */
public record RecommendedItem(
        Long productId,
        String name,
        BigDecimal price,
        String reason,
        double matchScore,
        Map<String, Object> attributes
) {
    /** 用于加权后返回新实例（record 不可变，走 with... 模式）*/
    public RecommendedItem withMatchScore(double s) {
        return new RecommendedItem(productId, name, price, reason, s, attributes);
    }

    public RecommendedItem withReason(String r) {
        return new RecommendedItem(productId, name, price, r, matchScore, attributes);
    }
}