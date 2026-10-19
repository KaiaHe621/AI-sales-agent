package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.entity.FaqEntryEntity;
import com.jichi.voiceshopping.repository.FaqEntryRepository;
import com.jichi.voiceshopping.service.FaqVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqVectorService faqVector;
    private final FaqEntryRepository faqRepo;

    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String q,
                                   @RequestParam(required = false, defaultValue = "0") Long merchantId) {
        // 阈值建议 0.80 起：低于这个值召回出来的 FAQ 大概率是"语义看起来像、实际答非所问"
        return faqVector.searchBest(q, merchantId, 0.80)
                .map(id -> {
                    faqRepo.incrHitCount(id);
                    FaqEntryEntity e = faqRepo.findById(id).orElseThrow();
                    return Map.<String, Object>of(
                            "hit", true,
                            "question", e.getQuestion(),
                            "answer", e.getAnswer(),
                            "category", e.getCategory());
                })
                .orElse(Map.of("hit", false));
    }
}