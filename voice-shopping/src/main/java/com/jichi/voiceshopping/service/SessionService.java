package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.entity.SessionEntity;
import com.jichi.voiceshopping.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository repo;

    @Transactional
    public SessionEntity openIfAbsent(String sessionId, Long userId, String channel) {
        return repo.findById(sessionId).orElseGet(() -> {
            SessionEntity s = new SessionEntity();
            s.setId(sessionId);
            s.setUserId(userId);
            s.setChannel(channel == null ? "HOME_ENTRY" : channel);
            s.setStartedAt(LocalDateTime.now());
            s.setLocale("zh_cn");
            s.setTotalTokens(0);
            return repo.save(s);
        });
    }

    public Long findUserId(String sessionId) {
        return repo.findById(sessionId).map(SessionEntity::getUserId).orElse(null);
    }

}