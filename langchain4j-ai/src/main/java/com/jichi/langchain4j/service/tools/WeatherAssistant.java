package com.jichi.langchain4j.service.tools;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface WeatherAssistant {

    @SystemMessage("""
            你是一个天气助手，可以查询实时天气和预报。
            根据用户需求决定是查实时天气还是预报，以及查几天的预报。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}