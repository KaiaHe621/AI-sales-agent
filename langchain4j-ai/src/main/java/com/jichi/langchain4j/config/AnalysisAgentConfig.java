package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.ownerAgent.AnalysisAgent;
import com.jichi.langchain4j.tools.ownerAgent.FinanceDataTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalysisAgentConfig {

    @Bean
    public AnalysisAgent analysisAgent(ChatModel chatModel, FinanceDataTools financeTools) {
        return AiServices.builder(AnalysisAgent.class)
                .chatModel(chatModel)
                .tools(unwrap(financeTools))
                // 多步推理上下文长，多留一些消息窗口
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(30))
                .build();
    }

    private Object unwrap(Object bean) {
        if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised) {
            try {
                return advised.getTargetSource().getTarget();
            } catch (Exception e) {
                throw new RuntimeException("无法解包 AOP 代理：" + bean.getClass(), e);
            }
        }
        return bean;
    }
}