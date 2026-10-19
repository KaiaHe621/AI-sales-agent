package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.agent.SmartAgent;
import com.jichi.langchain4j.tools.agent.ArithmeticMathTools;
import com.jichi.langchain4j.tools.agent.CityWeatherTools;
import com.jichi.langchain4j.tools.agent.WebSearchTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmartAgentConfig {

    @Bean
    public SmartAgent smartAgent(
            ChatModel chatModel,
            CityWeatherTools weatherTools,
            WebSearchTools searchTools,
            ArithmeticMathTools mathTools) {

        return AiServices.builder(SmartAgent.class)
                .chatModel(chatModel)
                // unwrap 剥掉 Spring AOP 的 CGLIB 代理，让 LangChain4j 能扫到 @Tool
                .tools(unwrap(weatherTools), unwrap(searchTools), unwrap(mathTools))
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(20))
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