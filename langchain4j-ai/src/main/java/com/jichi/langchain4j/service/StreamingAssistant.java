package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface StreamingAssistant {

    @SystemMessage("你是一个写作助手")
    TokenStream write(String topic);
}