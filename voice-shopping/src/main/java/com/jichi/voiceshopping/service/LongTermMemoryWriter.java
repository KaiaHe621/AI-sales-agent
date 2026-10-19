package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.SessionStateEntity;
import com.jichi.voiceshopping.entity.UserProfileDynamicEntity;
import com.jichi.voiceshopping.memory.ShortTermMemory;
import com.jichi.voiceshopping.repository.UserProfileDynamicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LongTermMemoryWriter {

    private final UserProfileDynamicRepository repo;
    private final SessionStateService stateService;
    private final ShortTermMemory shortMem;

    @Async
    public void flushOnSessionEnd(String sessionId, Long userId) {
        try {
            SessionStateEntity state = stateService.load(sessionId);
            var turns = shortMem.recent(sessionId, 50);
            if (turns.isEmpty()) return;

            UserProfileDynamicEntity dyn = repo.findById(userId).orElseGet(() -> initFor(userId));

            // 1. 品类偏好累加
            Object cat = state.getSlots() == null ? null : state.getSlots().get("category");
            if (cat != null) bumpAffinity(dyn.getCategoryAffinity(), (String) cat, 0.05);

            // 2. 价格敏感度：看本会话里 "便宜""贵" 出现次数
            long priceSensitiveHits = turns.stream()
                    .filter(t -> "USER".equals(t.role()))
                    .filter(t -> t.text() != null && (t.text().contains("便宜") || t.text().contains("贵")))
                    .count();
            if (priceSensitiveHits >= 2) {
                BigDecimal cur = dyn.getPriceSensitivity() == null ? BigDecimal.valueOf(0.5) : dyn.getPriceSensitivity();
                dyn.setPriceSensitivity(cur.add(BigDecimal.valueOf(0.1))
                        .min(BigDecimal.ONE));
            }

            repo.save(dyn);
            log.info("[LongMem] 回流 userId={} session={}", userId, sessionId);
        } catch (Exception e) {
            log.error("长期记忆回流失败 session={}", sessionId, e);
        }
    }

    private UserProfileDynamicEntity initFor(Long userId) {
        UserProfileDynamicEntity e = new UserProfileDynamicEntity();
        e.setUserId(userId);
        e.setCategoryAffinity(new HashMap<>());
        e.setBrandAffinity(new HashMap<>());
        e.setRecentViewed(new ArrayList<>());
        e.setRecentPurchased(new ArrayList<>());
        return e;
    }

    private void bumpAffinity(Map<String, Double> m, String key, double delta) {
        if (m == null) return;
        m.put(key, Math.min(1.0, m.getOrDefault(key, 0.0) + delta));
    }
}