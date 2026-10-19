package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.voice.TtsService;
import io.reactivex.Flowable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedTtsPhrases {

    private final TtsService tts;
    private final Map<String, byte[]> cache = new HashMap<>();

    @PostConstruct
    public void preload() {
        String[] phrases = {
                "鸡哥给你挑了三款。",
                "好的，稍等。",
                "这就帮你找。"
        };
        for (String p : phrases) {
            cache.put(p, synthesizeBlocking(p));
        }
        log.info("[Cost] TTS 短语缓存预热完成，共 {} 条", cache.size());
    }

    /** EmotionAgent 拿到第一个句子时调一下这个，命中就不走 TTS。 */
    public byte[] get(String phrase) { return cache.get(phrase); }

    private byte[] synthesizeBlocking(String text) {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        tts.synthesize(Flowable.just(text)).blockingForEach(buf -> {
            byte[] arr = new byte[buf.remaining()];
            buf.get(arr);
            bos.write(arr);
        });
        return bos.toByteArray();
    }
}