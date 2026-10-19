package com.jichi.springai.controller.chatMemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBudgetChatMemory implements ChatMemory {

    private static final int CHARS_PER_TOKEN = 4;  // 粗估：4个字符≈1 Token
    private final int maxTokenBudget;
    private final ConcurrentHashMap<String, List<Message>> store = new ConcurrentHashMap<>();

    public TokenBudgetChatMemory(int maxTokenBudget) {
        this.maxTokenBudget = maxTokenBudget;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        store.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
    }

    /**
     * Token 预算裁剪逻辑在这里实现：从最新消息往前累加，超出预算就截断。
     */
    @Override
    public List<Message> get(String conversationId) {
        List<Message> all = store.getOrDefault(conversationId, List.of());
        if (all.isEmpty()) return List.of();

        List<Message> result = new ArrayList<>();
        int tokenCount = 0;
        for (int i = all.size() - 1; i >= 0; i--) {
            int msgTokens = all.get(i).getText().length() / CHARS_PER_TOKEN;
            if (tokenCount + msgTokens > maxTokenBudget) break;
            result.add(all.get(i));
            tokenCount += msgTokens;
        }

        Collections.reverse(result);
        return result;
    }

    @Override
    public void clear(String conversationId) {
        store.remove(conversationId);
    }
}