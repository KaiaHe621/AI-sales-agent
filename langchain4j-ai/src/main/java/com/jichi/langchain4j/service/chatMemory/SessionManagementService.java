package com.jichi.langchain4j.service.chatMemory;

import com.jichi.langchain4j.memory.JpaChatMemoryStore;
import com.jichi.langchain4j.model.ChatMessageEntity;
import com.jichi.langchain4j.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionManagementService {

    private final ChatMessageRepository messageRepository;
    private final JpaChatMemoryStore memoryStore;

    public SessionManagementService(ChatMessageRepository messageRepository,
                                    JpaChatMemoryStore memoryStore) {
        this.messageRepository = messageRepository;
        this.memoryStore = memoryStore;
    }

    /**
     * 查询用户的所有历史会话（用第一条 USER 消息作为摘要）
     */
    public List<SessionSummary> getUserSessions(String userId) {
        String prefix = userId + "_";
        return messageRepository.findDistinctSessionIdsByPrefix(prefix)
                .stream()
                .map(sessionId -> {
                    List<ChatMessageEntity> messages =
                            messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
                    String summary = messages.stream()
                            .filter(m -> "USER".equals(m.getRole()))
                            .findFirst()
                            .map(m -> m.getContent().substring(0, Math.min(50, m.getContent().length())))
                            .orElse("新对话");
                    LocalDateTime lastActive = messages.isEmpty()
                            ? LocalDateTime.now()
                            : messages.get(messages.size() - 1).getCreatedAt();
                    return new SessionSummary(sessionId, summary, lastActive);
                })
                .toList();
    }

    /**
     * 删除会话（校验归属权，防止越权删除）
     */
    public void deleteSession(String sessionId, String userId) {
        if (!sessionId.startsWith(userId + "_")) {
            throw new SecurityException("无权删除此会话");
        }
        memoryStore.deleteMessages(sessionId);
    }

    /**
     * 生成新会话 ID
     */
    public String newSession(String userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    public record SessionSummary(String sessionId, String summary, LocalDateTime lastActive) {
    }
}