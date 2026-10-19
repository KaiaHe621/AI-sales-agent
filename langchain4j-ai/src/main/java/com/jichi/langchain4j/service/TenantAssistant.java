package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface TenantAssistant {

    @SystemMessage("你是{{companyName}}的客服助手，服务范围：{{serviceScope}}")
    String chat(@V("companyName") String company,
                @V("serviceScope") String scope,
                @UserMessage String userMessage);
}