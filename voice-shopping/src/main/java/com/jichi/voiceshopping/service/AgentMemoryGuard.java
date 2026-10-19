package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.agent.AgentFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentMemoryGuard {

    private final AgentFactory factory;

    /** 每分钟扫一遍所有活跃 session，把 memory 超 40 条的强制清掉。 */
    @Scheduled(fixedRate = 60_000)
    public void trimAllAgentMemories() {
        factory.forEachSession((sessionId, set) -> {
            int size = set.emotion().getMemory().getMessages().size();
            if (size > 40) {
                set.emotion().getMemory().clear();
                log.warn("[Cost] memory 超长清空 sessionId={} size={}", sessionId, size);
            }
        });
    }
}