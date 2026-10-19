package com.jichi.langchain4j.config;

import com.jichi.langchain4j.listener.ToolTraceLogger;
import com.jichi.langchain4j.service.tools.ArithmeticTools;
import com.jichi.langchain4j.service.tools.TraceAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceAssistantConfig {

    @Bean
    public TraceAssistant traceAssistant(
            ChatModel model,
            ArithmeticTools arithmeticTools,
            ToolTraceLogger toolTraceLogger) throws Exception {

        // ToolExceptionAspect 会生成 CGLIB 代理，LangChain4j 反射扫描 @Tool 时需先解包
        Object rawTools = AopUtils.isAopProxy(arithmeticTools)
                ? ((Advised) arithmeticTools).getTargetSource().getTarget()
                : arithmeticTools;

        return AiServices.builder(TraceAssistant.class)
                .chatModel(model)
                .tools(rawTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .afterToolExecution(toolTraceLogger)   // 工具执行完毕后触发
                .build();
    }
}