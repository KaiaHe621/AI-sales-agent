package com.jichi.voiceshopping.listener;

import com.jichi.voiceshopping.service.LongTermMemoryWriter;
import com.jichi.voiceshopping.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionExpireListener extends KeyExpirationEventMessageListener {

    private final LongTermMemoryWriter writer;
    private final SessionService sessionService;

    public SessionExpireListener(RedisMessageListenerContainer container,
                                 LongTermMemoryWriter writer,
                                 SessionService sessionService) {
        super(container);
        this.writer = writer;
        this.sessionService = sessionService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        if (!expiredKey.startsWith("vs:session:")) return;

        String sessionId = expiredKey.substring("vs:session:".length());
        // userId 从 PG 里的 session 主表查（Redis 已过期拿不到）
        Long userId = sessionService.findUserId(sessionId);
        if (userId != null) {
            log.info("[Listener] session expired sessionId={} userId={}, flush memory", sessionId, userId);
            writer.flushOnSessionEnd(sessionId, userId);
        }
    }
}