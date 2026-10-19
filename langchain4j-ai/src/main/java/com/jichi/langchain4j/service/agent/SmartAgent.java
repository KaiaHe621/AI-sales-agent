package com.jichi.langchain4j.service.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface SmartAgent {

    @SystemMessage("""
            你是一个智能助手，拥有以下工具：
            - 查天气：可以获取任何城市的实时天气
            - 搜索：可以搜索互联网获取信息
            - 计算：可以执行加减乘除运算
            
            根据用户的问题，决定是否需要使用工具。
            需要多个信息时，可以多次调用工具。
            综合所有工具的返回结果，给出准确的最终答案。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}