package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.SessionScope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ScopeFilterBuilder {

    /**
     * 把 SessionScope 翻译成 SQL 过滤片段 + 参数
     */
    public SqlFilterBuilder.Filter build(SessionScope scope) {
        if (scope == null || scope.isPlatformWide()) {
            return new SqlFilterBuilder.Filter("", List.of());
        }
        List<Long> ids = scope.allowedMerchantIds();
        // 生成形如 "merchant_id IN (?,?,?)" 的片段，参数分别绑定
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        List<Object> params = new java.util.ArrayList<>(ids);
        return new SqlFilterBuilder.Filter("merchant_id IN (" + placeholders + ")", params);
    }
}