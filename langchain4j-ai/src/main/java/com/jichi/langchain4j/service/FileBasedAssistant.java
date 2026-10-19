package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface FileBasedAssistant {

    // fromResource 指定加载的文件路径
    @SystemMessage(fromResource = "prompts/customer-service.txt")
    String chat(@V("companyName") String company,
                @V("serviceScope") String scope,
                @UserMessage String message);
}