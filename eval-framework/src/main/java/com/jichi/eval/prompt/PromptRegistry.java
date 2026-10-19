package com.jichi.eval.prompt;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PromptRegistry {

    private final Map<String, PromptVersion> versions = new LinkedHashMap<>();

    public void register(String version, String systemPrompt, String description) {
        versions.put(version, PromptVersion.builder()
                .version(version)
                .systemPrompt(systemPrompt)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public Optional<PromptVersion> get(String version) {
        return Optional.ofNullable(versions.get(version));
    }

    public Map<String, PromptVersion> listAll() {
        return Map.copyOf(versions);
    }
}