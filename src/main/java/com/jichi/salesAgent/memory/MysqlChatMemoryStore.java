package com.jichi.salesAgent.memory;

import com.jichi.salesAgent.entity.ChatMemoryEntity;
import com.jichi.salesAgent.repository.ChatMemoryRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MysqlChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryRepository repository;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        return repository.findBySessionId(sessionId)
                .map(entity -> {
                    try {
                        return ChatMessageDeserializer.messagesFromJson(entity.getMessages());
                    } catch (Exception e) {
                        log.warn("反序列化对话记忆失败，sessionId={}", sessionId, e);
                        return Collections.<ChatMessage>emptyList();
                    }
                })
                .orElse(Collections.emptyList());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            ChatMemoryEntity entity = repository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        ChatMemoryEntity e = new ChatMemoryEntity();
                        e.setSessionId(sessionId);
                        return e;
                    });
            entity.setMessages(json);
            repository.save(entity);
        } catch (Exception e) {
            log.error("保存对话记忆失败，sessionId={}", sessionId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessages(Object memoryId) {
        repository.deleteBySessionId(memoryId.toString());
    }
}
