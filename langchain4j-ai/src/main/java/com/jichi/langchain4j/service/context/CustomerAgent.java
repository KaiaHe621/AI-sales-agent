package com.jichi.langchain4j.service.context;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CustomerAgent {

    @SystemMessage("你是用户的专属客服，帮助查询订单和处理售后。查询订单时直接调用工具，不需要用户提供ID。")
    String chat(@UserMessage String message);
}