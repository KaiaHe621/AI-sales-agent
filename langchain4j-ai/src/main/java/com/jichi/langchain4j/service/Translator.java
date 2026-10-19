package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Translator {

    @SystemMessage("你是一个专业翻译")
    @UserMessage("将以下文字翻译成{{language}}：\n{{text}}")
    String translate(@V("language") String language, @V("text") String text);
}