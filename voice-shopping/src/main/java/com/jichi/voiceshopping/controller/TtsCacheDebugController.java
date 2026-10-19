package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.service.CachedTtsPhrases;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug/tts-cache")
@RequiredArgsConstructor
public class TtsCacheDebugController {

    private final CachedTtsPhrases cache;

    @GetMapping("/probe")
    public Map<String, Object> probe(@RequestParam String phrase) {
        byte[] pcm = cache.get(phrase);
        return Map.of("phrase", phrase, "hit", pcm != null,
                "bytes", pcm == null ? 0 : pcm.length);
    }
}