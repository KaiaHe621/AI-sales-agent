package com.jichi.langchain4j.service;

import com.jichi.langchain4j.model.TicketCategory;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface TicketClassifier {

    @SystemMessage("""
            对客户工单进行分类。
            BILLING：账单/付款问题
            TECH_SUPPORT：技术故障
            FEATURE_REQUEST：功能建议
            ACCOUNT：账号/权限问题
            OTHER：其他
            """)
    TicketCategory classify(String ticket);
}