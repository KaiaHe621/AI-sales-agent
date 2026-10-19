package com.jichi.langchain4j.service.tools;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TimeoutWeatherAssistant {

    @SystemMessage("你是一个天气查询助手，用户询问天气时必须调用工具获取数据，不得凭空回答。")
    String chat(@UserMessage String message);
}