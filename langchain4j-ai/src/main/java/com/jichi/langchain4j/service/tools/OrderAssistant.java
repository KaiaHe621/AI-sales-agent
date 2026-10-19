package com.jichi.langchain4j.service.tools;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface OrderAssistant {

    @SystemMessage("""
            你是一个订单查询助手，可以查询用户信息、订单状态和统计数据。
            根据用户问题判断需要调用哪个工具获取数据。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}