package com.jichi.langchain4j.memory;

import com.jichi.langchain4j.model.ChatMessageEntity;
import com.jichi.langchain4j.repository.ChatMessageRepository;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
public class JpaChatMemoryStore implements ChatMemoryStore {

    private final ChatMessageRepository repository;

    public JpaChatMemoryStore(ChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return repository.findBySessionIdOrderByCreatedAtAsc(memoryId.toString())
                .stream()
                .map(this::toMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 全量替换：先删后插
        // 生产环境可以优化为增量更新（只追加新消息），减少写放大
        repository.deleteBySessionId(memoryId.toString());

        List<ChatMessageEntity> entities = messages.stream()
                .map(msg -> toEntity(memoryId.toString(), msg))
                .toList();

        repository.saveAll(entities);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        repository.deleteBySessionId(memoryId.toString());
    }

    private ChatMessageEntity toEntity(String sessionId, ChatMessage message) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionId(sessionId);

        if (message instanceof SystemMessage m) {
            entity.setRole("SYSTEM");
            entity.setContent(m.text());
        } else if (message instanceof UserMessage m) {
            entity.setRole("USER");
            entity.setContent(m.singleText());
        } else if (message instanceof AiMessage m) {
            entity.setRole("AI");
            entity.setContent(m.text() != null ? m.text() : "");
        } else if (message instanceof ToolExecutionResultMessage m) {
            entity.setRole("TOOL");
            entity.setContent(m.text());
            entity.setToolName(m.toolName());
        }

        return entity;
    }

    private ChatMessage toMessage(ChatMessageEntity entity) {
        return switch (entity.getRole()) {
            case "SYSTEM" -> new SystemMessage(entity.getContent());
            case "USER" -> new UserMessage(entity.getContent());
            case "AI" -> new AiMessage(entity.getContent());
            case "TOOL" -> new ToolExecutionResultMessage(
                    entity.getToolName(), entity.getToolName(), entity.getContent());
            default -> null;
        };
    }
}