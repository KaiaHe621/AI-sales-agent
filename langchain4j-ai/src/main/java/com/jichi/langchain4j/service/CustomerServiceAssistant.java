package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CustomerServiceAssistant {

    @SystemMessage("""
            你是"鸡翅商城"的智能客服助手小鸡。
            
            服务范围：商品咨询、订单查询、售后服务
            
            约束：
            - 只回答与极致商城相关的问题
            - 不确定的信息说"我帮您查一下"，不要猜测
            - 回复不超过 150 字
            - 涉及投诉主动提出转人工
            """)
    String chat(String userMessage);
}