package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductTextBuilder {

    public String build(ProductEntity p) {
        String attrText = p.getAttributes() == null ? "" : humanize(p.getAttributes());
        String sellingPoints = p.getSellingPoints() == null ? "" : p.getSellingPoints();
        return String.join("。",
                p.getName(),
                p.getBrand() + " " + p.getCategoryL2(),
                sellingPoints,
                sellingPoints,    // 有意重复，提权
                p.getDescription() == null ? "" : p.getDescription(),
                attrText
        );
    }

    private String humanize(Map<String, Object> attrs) {
        return attrs.entrySet().stream()
                .map(e -> toChinese(e.getKey()) + "是" + e.getValue())
                .collect(Collectors.joining("，"));
    }

    private String toChinese(String key) {
        return switch (key) {
            case "cushion"    -> "缓震";
            case "weight"     -> "重量";
            case "gender"     -> "适合性别";
            case "waterproof" -> "防水";
            case "finish"     -> "妆感";
            case "color"      -> "颜色";
            case "movement"   -> "机芯";
            case "material"   -> "材质";
            default -> key;
        };
    }
}