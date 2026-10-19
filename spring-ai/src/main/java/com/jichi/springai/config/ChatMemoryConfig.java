package com.jichi.springai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.springai.memory.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        RedisChatMemoryRepository repository = new RedisChatMemoryRepository(redisTemplate, objectMapper);
        // 底层走 Redis 持久化，上层限制最多保留 20 条消息
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }
}