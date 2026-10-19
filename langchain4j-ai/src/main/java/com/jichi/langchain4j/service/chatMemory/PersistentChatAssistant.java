package com.jichi.langchain4j.service.chatMemory;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface PersistentChatAssistant {
    @SystemMessage("你是一个 Java 技术助手，记住用户在对话中提到的技术栈和问题背景")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}