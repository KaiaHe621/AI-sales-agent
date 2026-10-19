package com.jichi.springai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ChatMemoryRepository  —— 纯存储层，负责读写所有消息，不做裁剪
 * MessageWindowChatMemory —— 包装 Repository，对外暴露 ChatMemory，负责按条数裁剪
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final int TTL_DAYS = 7;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 追加消息并刷新过期时间
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        // 先删除旧数据，再写入完整列表
        redisTemplate.delete(key);
        for (Message message : messages) {
            try {
                MessageRecord record = new MessageRecord(
                        message.getMessageType().name(),
                        message.getText()
                );
                redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(record));
            } catch (Exception e) {
                throw new RuntimeException("存储消息失败", e);
            }
        }
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 返回该会话的全部消息，裁剪逻辑由外层 MessageWindowChatMemory 处理
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> rawMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (rawMessages == null) return new ArrayList<>();

        List<Message> messages = new ArrayList<>();
        for (String raw : rawMessages) {
            try {
                MessageRecord record = objectMapper.readValue(raw, MessageRecord.class);
                if ("USER".equals(record.role())) {
                    messages.add(new UserMessage(record.content()));
                } else if ("ASSISTANT".equals(record.role())) {
                    messages.add(new AssistantMessage(record.content()));
                }
            } catch (Exception ignored) {
            }
        }
        return messages;
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    /**
     * 返回所有会话 ID（扫描 Redis 中匹配前缀的 key）
     */
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return new ArrayList<>();
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .toList();
    }

    record MessageRecord(String role, String content) {
    }
}