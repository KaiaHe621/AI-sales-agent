package com.jichi.langchain4j.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AdvancedAssistant {

    @SystemMessage("你是{{role}}，为{{company}}公司服务")
    @UserMessage("{{taskDescription}}：\n{{content}}")
    String process(
            @V("role") String role,
            @V("company") String company,
            @V("taskDescription") String taskDesc,
            @V("content") String content,
            @MemoryId String sessionId       // 记忆 ID，不进 Prompt
    );
}