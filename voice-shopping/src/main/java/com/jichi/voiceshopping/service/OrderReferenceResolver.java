package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.SessionStateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderReferenceResolver {

    private static final Pattern ORDINAL = Pattern.compile("第?([一二三四五1-5])款?");

    /**
     * 尝试从用户话里抽取想下单的商品 id。
     * 解析失败返回 empty，Orchestrator 让 EmotionAgent 去追问。
     */
    public Optional<Long> resolve(SessionStateEntity state, String utterance) {
        if (state.getLastRecommendations() == null || state.getLastRecommendations().isEmpty()) {
            return Optional.empty();
        }
        List<Long> last = state.getLastRecommendations();

        // 1. 序数词 "第二款"
        Matcher m = ORDINAL.matcher(utterance);
        if (m.find()) {
            int idx = ordinalToIndex(m.group(1));
            if (idx >= 0 && idx < last.size()) return Optional.of(last.get(idx));
        }

        // 2. 就/要/选 + 开头 / 最后 / 中间
        if (utterance.contains("第一") || utterance.contains("开头")) return Optional.of(last.get(0));
        if (utterance.contains("最后") || utterance.contains("最后那")) return Optional.of(last.get(last.size() - 1));
        if (utterance.contains("中间")) return Optional.of(last.get(last.size() / 2));

        // 3. 用户说了具体商品名或品牌（模糊匹配）
        // ... 留给后续扩展，可用一个简单的模糊匹配打分

        return Optional.empty();
    }

    private int ordinalToIndex(String ord) {
        return switch (ord) {
            case "一", "1" -> 0;
            case "二", "2" -> 1;
            case "三", "3" -> 2;
            case "四", "4" -> 3;
            case "五", "5" -> 4;
            default -> -1;
        };
    }
}