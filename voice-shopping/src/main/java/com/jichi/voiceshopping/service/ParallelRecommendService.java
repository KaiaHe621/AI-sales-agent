package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParallelRecommendService {

    private final UserProfileService profileService;
    private final RecommendCandidatesService candidates;
    private final ProfileReranker reranker;
    private final RecommendReasonService reasonService;
    private final SessionScopeCache scopeCache;      // 新增

    public RecommendResult recommend(String sessionId, Long userId,
                                     String utterance, Map<String, Object> slots) {

        SessionScope scope = scopeCache.get(sessionId);
        if (scope == null) scope = new SessionScope(userId, null, null); // 老会话兜底

        final SessionScope finalScope = scope; // lambda 要 effectively final

        CompletableFuture<UserProfileSnapshot> profileFuture =
                CompletableFuture.supplyAsync(() -> profileService.load(userId));

        CompletableFuture<List<RecommendedItem>> candidatesFuture =
                CompletableFuture.supplyAsync(() -> candidates.fetchCandidates(
                        utterance, slots, finalScope, 20));   // ← 传 scope

        List<RecommendedItem> reranked = profileFuture
                .thenCombine(candidatesFuture, (profile, cands) -> reranker.rerank(cands, profile, slots))
                .join();

        if (reranked.isEmpty()) return new RecommendResult(List.of(), "empty");

        List<RecommendedItem> top3 = reranked.stream().limit(3).toList();
        return new RecommendResult(
                reasonService.attachReasons(sessionId, utterance + "; " + slots, top3),
                "professional");
    }
}