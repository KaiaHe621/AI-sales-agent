package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.dto.UserProfileSnapshot;
import com.jichi.voiceshopping.entity.UserProfileDynamicEntity;
import com.jichi.voiceshopping.entity.UserProfileStaticEntity;
import com.jichi.voiceshopping.repository.UserProfileDynamicRepository;
import com.jichi.voiceshopping.repository.UserProfileStaticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileStaticRepository staticRepo;
    private final UserProfileDynamicRepository dynRepo;

    @Cacheable(value = "userProfileSnapshot", key = "#userId")
    public UserProfileSnapshot load(Long userId) {
        Optional<UserProfileStaticEntity> s = staticRepo.findById(userId);
        Optional<UserProfileDynamicEntity> d = dynRepo.findById(userId);

        return new UserProfileSnapshot(
                userId,
                s.map(UserProfileStaticEntity::getGender).orElse(null),
                s.map(UserProfileStaticEntity::getAge).orElse(null),
                s.map(UserProfileStaticEntity::getHeightCm).orElse(null),
                s.map(UserProfileStaticEntity::getWeightKg).orElse(null),
                s.map(UserProfileStaticEntity::getSkinType).orElse(null),
                s.map(UserProfileStaticEntity::getBudgetBand).orElse(null),
                d.map(UserProfileDynamicEntity::getCategoryAffinity).orElse(Map.of()),
                d.map(UserProfileDynamicEntity::getBrandAffinity).orElse(Map.of()),
                d.map(UserProfileDynamicEntity::getRecentViewed).orElse(List.of()),
                d.map(UserProfileDynamicEntity::getRecentPurchased).orElse(List.of()),
                d.map(UserProfileDynamicEntity::getPriceSensitivity).orElse(null),
                d.map(UserProfileDynamicEntity::getAvgOrderAmount).orElse(null)
        );
    }
}