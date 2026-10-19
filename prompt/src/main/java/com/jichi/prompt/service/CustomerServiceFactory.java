package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.CustomerServiceConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class CustomerServiceFactory {

    private final DashScopeChatModel chatModel;

    public CustomerServiceFactory(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public ChatClient createForTenant(CustomerServiceConfig config) {
        Resource systemPromptResource = new ClassPathResource("prompts/customer-service-system.st");

        PromptTemplate pt = new PromptTemplate(systemPromptResource);
        String systemPrompt = pt.render(Map.of(
                "companyName", config.companyName(),
                "assistantName", config.assistantName(),
                "serviceScope", String.join("\n- ", config.serviceScope()),
                "sensitiveTopics", String.join("、", config.sensitiveTopics()),
                "tone", config.tone(),
                "currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        ));

        // 用 ChatClient.builder(chatModel) 代替自动注入的 Builder
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
    }
}