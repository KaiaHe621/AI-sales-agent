package com.jichi.langchain4j.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

// TenantChatAssistant 是不加 @AiService 的纯接口，由 AiServices.builder() 编程式构建
public interface TenantChatAssistant {
    String chat(@MemoryId String sessionId, @UserMessage String message);
}