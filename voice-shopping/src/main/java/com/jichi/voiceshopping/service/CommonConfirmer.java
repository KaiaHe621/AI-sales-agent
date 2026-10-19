package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.Intent;
import com.jichi.voiceshopping.dto.IntentResult;

import java.util.Map;
import java.util.Set;

final class CommonConfirmer {

    /**
     * 高频简短确认词——这些词意图基本固定为 ORDER_CONFIRM / 继续推荐。
     */
    private static final Set<String> WORDS = Set.of(
            "嗯", "好", "好的", "对", "是", "行", "可以",
            "继续", "下一个", "再来", "换一个"
    );

    static final IntentResult CONFIRMER_INTENT =
            new IntentResult(Intent.ORDER_CONFIRM, Map.of("confirmer", true), 0.95);

    static boolean isCommonConfirmer(String utterance) {
        if (utterance == null) return false;
        String s = utterance.trim().replaceAll("[，。！？,.!?\\s]", "");
        return s.length() <= 3 && WORDS.contains(s);
    }
}