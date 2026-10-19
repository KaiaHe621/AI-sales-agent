package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.UserProfileDynamicEntity;
import com.jichi.voiceshopping.repository.UserProfileDynamicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBehaviorSink {

    private final UserProfileDynamicRepository repo;

    public record ProductViewedEvent(Long userId, Long productId, String category) {}
    public record ProductPurchasedEvent(Long userId, Long productId, String category, double amount) {}

    @Async
    @EventListener
    public void onViewed(ProductViewedEvent e) {
        UserProfileDynamicEntity p = repo.findById(e.userId()).orElseGet(() -> initFor(e.userId()));
        List<Long> viewed = new ArrayList<>(p.getRecentViewed() == null ? List.of() : p.getRecentViewed());
        viewed.add(0, e.productId());
        if (viewed.size() > 100) viewed = viewed.subList(0, 100);
        p.setRecentViewed(viewed);
        updateAffinity(p.getCategoryAffinity(), e.category(), 0.05);
        repo.save(p);
    }

    @Async
    @EventListener
    public void onPurchased(ProductPurchasedEvent e) {
        UserProfileDynamicEntity p = repo.findById(e.userId()).orElseGet(() -> initFor(e.userId()));
        List<Long> bought = new ArrayList<>(p.getRecentPurchased() == null ? List.of() : p.getRecentPurchased());
        bought.add(0, e.productId());
        if (bought.size() > 100) bought = bought.subList(0, 100);
        p.setRecentPurchased(bought);
        updateAffinity(p.getCategoryAffinity(), e.category(), 0.15);
        repo.save(p);
    }

    private UserProfileDynamicEntity initFor(Long userId) {
        UserProfileDynamicEntity e = new UserProfileDynamicEntity();
        e.setUserId(userId);
        e.setCategoryAffinity(new java.util.HashMap<>());
        e.setBrandAffinity(new java.util.HashMap<>());
        e.setRecentViewed(new ArrayList<>());
        e.setRecentPurchased(new ArrayList<>());
        return e;
    }

    private void updateAffinity(java.util.Map<String, Double> map, String key, double delta) {
        if (key == null || map == null) return;
        double cur = map.getOrDefault(key, 0.0);
        map.put(key, Math.min(1.0, cur + delta));
    }
}