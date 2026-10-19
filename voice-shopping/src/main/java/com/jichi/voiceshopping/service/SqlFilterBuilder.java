package com.jichi.voiceshopping.service;

import java.util.*;

public class SqlFilterBuilder {

    public record Filter(String clause, List<Object> params) {}

    public static Filter fromSlots(Map<String, Object> slots) {
        List<String> frags = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (slots == null) return new Filter("", params);

        Object category = slots.get("category");
        if (category != null) {
            frags.add("category_l2 = ?");
            params.add(category.toString());
        }

        Object budget = slots.get("budget");
        if (budget instanceof Number n) {
            frags.add("price <= ?");
            params.add(n.doubleValue());
        }

        // 对比类场景下界：expensive 方向用，把低于下界的都过滤掉
        Object priceMin = slots.get("priceMin");
        if (priceMin instanceof Number n) {
            frags.add("price >= ?");
            params.add(n.doubleValue());
        }

        Object gender = slots.get("gender");
        if (gender != null) {
            frags.add("attributes->>'gender' IN (?, 'unisex')");
            params.add(gender.toString());
        }

        Object brand = slots.get("brand");
        if (brand != null) {
            frags.add("brand = ?");
            params.add(brand.toString());
        }

        // 排除上一轮已推过的商品，避免"换一款/便宜点"的时候又把同一批推回来
        Object exclude = slots.get("excludeProductIds");
        if (exclude instanceof Collection<?> c && !c.isEmpty()) {
            String placeholders = String.join(", ", Collections.nCopies(c.size(), "?"));
            frags.add("id NOT IN (" + placeholders + ")");
            params.addAll(c);
        }

        return new Filter(String.join(" AND ", frags), params);
    }

    /** 用户场景映射到商品属性（跑鞋专用示例）。
     *  cushion 和 terrain 要一起过滤——否则"水泥路"场景会被越野鞋（trail）
     *  误命中（越野鞋 cushion 常常也是 high，单看 cushion 挡不住）。
     *  terrain 条件用 `IS NULL OR IN (...)` 让未标注 terrain 的商品（数据不全常态）也能入围，
     *  只明确排除跟场景强冲突的类型（比如水泥路不能推 trail）。 */
    public static Filter runningShoeFilter(Map<String, Object> slots) {
        String scenario = (String) slots.get("scenario");
        if (scenario == null) return new Filter("", List.of());
        return switch (scenario) {
            case "水泥路"    -> new Filter(
                    "attributes->>'cushion' IN ('high','medium') " +
                    "AND (attributes->>'terrain' IS NULL OR attributes->>'terrain' IN ('road'))",
                    List.of());
            case "塑胶跑道" -> new Filter(
                    "attributes->>'cushion' IN ('medium','low') " +
                    "AND (attributes->>'terrain' IS NULL OR attributes->>'terrain' IN ('road','track'))",
                    List.of());
            case "越野"      -> new Filter(
                    "attributes->>'cushion' = 'high' " +
                    "AND attributes->>'terrain' = 'trail'", List.of());
            default          -> new Filter("", List.of());
        };
    }

    public static Filter merge(Filter a, Filter b) {
        if (a.clause.isBlank()) return b;
        if (b.clause.isBlank()) return a;
        List<Object> merged = new ArrayList<>(a.params);
        merged.addAll(b.params);
        return new Filter(a.clause + " AND " + b.clause, merged);
    }
}