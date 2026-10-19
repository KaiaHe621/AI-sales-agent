package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.context.CustomerAgent;
import com.jichi.langchain4j.tools.context.UserContextTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CustomerAgentConfig {

    @Bean
    @RequestScope
    public CustomerAgent customerAgent(ChatModel model, HttpServletRequest request) {
        // 直接 new，传入当前请求，完全绕过 Spring 代理链
        // 不通过 Spring 注入 UserContextTools，是为了避免双层代理问题：
        // @RequestScope 代理 + ToolExceptionAspect AOP 代理叠加后，
        // LangChain4j 拿到的是 UserContextTools$$SpringCGLIB$$N，扫不到 @Tool。
        UserContextTools tools = new UserContextTools(request);

        return AiServices.builder(CustomerAgent.class)
                .chatModel(model)
                .tools(tools)
                .build();
    }
}