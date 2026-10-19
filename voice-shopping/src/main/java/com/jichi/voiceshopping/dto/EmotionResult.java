package com.jichi.voiceshopping.dto;

import java.util.List;

public record EmotionResult(
        String speechText,
        List<RecommendedItem> displayBlocks
) {}