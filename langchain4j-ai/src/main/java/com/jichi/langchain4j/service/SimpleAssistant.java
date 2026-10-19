package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService  // 告诉 LangChain4j：这是一个 AI 服务，帮我生成实现
public interface SimpleAssistant {

    @SystemMessage("你是一个友好的 AI 助手，用简洁的语言回答问题")
    String chat(String userMessage);
}