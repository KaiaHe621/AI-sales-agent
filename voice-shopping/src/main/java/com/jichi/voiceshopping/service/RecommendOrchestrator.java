package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendOrchestrator {

    private final RecommendCandidatesService candidates;
    private final ProfileReranker reranker;
    private final RecommendReasonService reasonService;
    private final UserProfileService profileService;
    private final SessionScopeCache scopeCache;

    public RecommendResult recommend(String sessionId, Long userId,
                                     String utterance, Map<String, Object> slots) {
        UserProfileSnapshot profile = profileService.load(userId);
        String query = buildQuery(utterance, slots);

        SessionScope scope = scopeCache.get(sessionId);
        if (scope == null) {
            // 没 scope 兜底成"仅该用户、全平台"，避免老会话直接挂
            scope = new SessionScope(userId, null, null);
        }

        List<RecommendedItem> top20 = candidates.fetchCandidates(query, slots, scope, 20);
        if (top20.isEmpty()) {
            // 1. 放宽预算 +30%
            Map<String, Object> relaxed = new java.util.HashMap<>(slots);
            if (slots.get("budget") instanceof Number n) {
                relaxed.put("budget", n.intValue() * 1.3);
            }
            top20 = candidates.fetchCandidates(query, relaxed, scope, 20);

            // 2. 放宽品类（丢掉二级分类）
            if (top20.isEmpty()) {
                relaxed.remove("category");
                top20 = fallback(query, slots, scope);
            }

            // 3. 还是空，标记 explanationTone=empty
            if (top20.isEmpty()) return new RecommendResult(List.of(), "empty");
        }

        List<RecommendedItem> reranked = reranker.rerank(top20, profile, slots);
        List<RecommendedItem> top3 = reranked.stream().limit(3).collect(Collectors.toList());

        //String userNeeds = utterance + "; 槽位：" + slots;
        //List<RecommendedItem> withReasons = reasonService.attachReasons(sessionId, userNeeds, top3);

        //return new RecommendResult(withReasons, "professional");
        return new RecommendResult(top3, "professional");

    }

    private List<RecommendedItem> fallback(String query, Map<String, Object> slots, SessionScope scope) {
        Map<String, Object> relaxed = new HashMap<>(slots);
        if (slots.get("budget") instanceof Number n) {
            relaxed.put("budget", n.intValue() * 1.3);
        }
        List<RecommendedItem> r = candidates.fetchCandidates(query, relaxed, scope, 20);
        if (r.isEmpty()) {
            relaxed.remove("category");
            r = candidates.fetchCandidates(query, relaxed, scope, 10);
        }
        return r;
    }

    private String buildQuery(String utterance, Map<String, Object> slots) {
        StringBuilder sb = new StringBuilder(utterance);
        if (slots != null) {
            Object scenario = slots.get("scenario");
            if (scenario != null) sb.append(" ").append(scenario);
        }
        return sb.toString();
    }
}