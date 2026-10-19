package com.jichi.voiceshopping.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ClarifyRuleService {

    private Map<String, CategoryRule> rules;

    public ClarifyRuleService() throws Exception {
        try (InputStream in = new ClassPathResource("clarify/required-slots.yml").getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Map<String, Object>> raw = yaml.load(in);
            rules = new java.util.HashMap<>();
            for (var e : raw.entrySet()) {
                CategoryRule r = new CategoryRule();
                r.setRequired((List<String>) e.getValue().get("required"));
                r.setNiceToHave((List<String>) e.getValue().getOrDefault("nice_to_have", List.of()));
                rules.put(e.getKey(), r);
            }
        }
    }

    public List<String> missingSlots(String category, Map<String, Object> slots) {
        CategoryRule rule = rules.getOrDefault(category, rules.get("default"));
        List<String> missing = new java.util.ArrayList<>();
        for (String req : rule.getRequired()) {
            if (slots == null || slots.get(req) == null) missing.add(req);
        }
        return missing;
    }

    @Data
    public static class CategoryRule {
        private List<String> required;
        private List<String> niceToHave;
    }
}