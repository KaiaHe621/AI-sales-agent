package com.jichi.voiceshopping.dto;

/** 会话入口类型，决定本次会话的商家范围 */
public enum Channel {
    HOME_ENTRY,       // 平台首页 → 全平台
    PRODUCT_PAGE,     // 商详页 → 锁定到商品所属商家
    MERCHANT_HOME,    // 商家店铺首页 → 锁定到该商家
    SEARCH_FALLBACK   // 搜索无结果兜底 → 全平台
}