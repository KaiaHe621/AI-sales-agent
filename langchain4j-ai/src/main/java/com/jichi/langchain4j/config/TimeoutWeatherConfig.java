package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.tools.TimeoutWeatherAssistant;
import com.jichi.langchain4j.service.tools.TimeoutWeatherTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeoutWeatherConfig {

    @Bean
    public TimeoutWeatherAssistant timeoutWeatherAssistant(
            ChatModel model,
            TimeoutWeatherTools timeoutWeatherTools) throws Exception {

        Object rawTools = AopUtils.isAopProxy(timeoutWeatherTools)
                ? ((Advised) timeoutWeatherTools).getTargetSource().getTarget()
                : timeoutWeatherTools;

        return AiServices.builder(TimeoutWeatherAssistant.class)
                .chatModel(model)
                .tools(rawTools)
                .build();
    }
}