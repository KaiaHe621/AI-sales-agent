package com.jichi.voiceshopping.dto;

import java.util.List;

public record ClarifyResult(
        Action action,
        String questionToAsk,
        List<String> missingSlots
) {
    public enum Action { ASK, READY }

    public static ClarifyResult ready() {
        return new ClarifyResult(Action.READY, null, List.of());
    }
    public static ClarifyResult ask(String q, List<String> missing) {
        return new ClarifyResult(Action.ASK, q, missing);
    }
}