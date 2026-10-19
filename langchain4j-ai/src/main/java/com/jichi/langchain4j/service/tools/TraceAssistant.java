package com.jichi.langchain4j.service.tools;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

// 不加 @AiService，由 TraceAssistantConfig 手动构建
public interface TraceAssistant {

    @SystemMessage("你是一个计算助手，用户提出数学计算时必须调用工具完成，不得自行心算。")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}