package com.jichi.voiceshopping.event;

import com.jichi.voiceshopping.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceEventListeners {

    private final UserProfileService profileService;

    @Async
    @EventListener
    public void onUserSpokenWarmup(UserSpokenEvent event) {
        try {
            profileService.load(event.userId());
            log.info("[WARMUP] userId={} 画像已预热", event.userId());
        } catch (Exception e) {
            log.warn("画像预热失败 userId={}", event.userId(), e);
        }
    }

    @Async
    @EventListener
    public void onUserSpokenAudit(UserSpokenEvent event) {
        log.info("[AUDIT] sessionId={} userId={} utterance={}",
                event.sessionId(), event.userId(), event.utterance());
    }
}