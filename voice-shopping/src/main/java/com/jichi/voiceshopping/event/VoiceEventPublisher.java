package com.jichi.voiceshopping.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoiceEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishUserSpoken(String sessionId, Long userId, String utterance) {
        publisher.publishEvent(new UserSpokenEvent(sessionId, userId, utterance, System.currentTimeMillis()));
    }
}