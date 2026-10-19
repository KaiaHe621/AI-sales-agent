package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.tools.WeatherAssistant;
import com.jichi.langchain4j.tools.WeatherTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WeatherAssistantConfig {

    @Bean
    @Primary
    public WeatherAssistant weatherAssistantWithTools(ChatModel model,
                                                      WeatherTools weatherTools) {
        return AiServices.builder(WeatherAssistant.class)
                .chatModel(model)
                .tools(weatherTools)
                // @MemoryId 场景必须用 chatMemoryProvider，否则 NPE
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}