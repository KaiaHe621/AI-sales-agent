package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Assistant {

    @SystemMessage("你是一个 Java 技术助手，用简洁的语言回答，代码用 Java 17 语法")
    String chat(String question);
}