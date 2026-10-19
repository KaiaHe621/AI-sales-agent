package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TicketClassificationService {

    private final ChatClient chatClient;

    private static final String CLASSIFICATION_PROMPT = """
            将客户工单分类到以下类别之一：
            - BILLING（账单/付款问题）
            - TECH_SUPPORT（技术故障）
            - FEATURE_REQUEST（功能建议）
            - ACCOUNT（账号/权限问题）
            - OTHER（其他）
            
            只输出类别名称，不要有其他内容。
            
            示例1：
            工单：我的信用卡被扣了两次钱
            类别：BILLING
            
            示例2：
            工单：登录页面报500错误，一直进不去
            类别：TECH_SUPPORT
            
            示例3：
            工单：希望能支持批量导出功能
            类别：FEATURE_REQUEST
            
            示例4：
            工单：我的账号被锁了，忘记密码了
            类别：ACCOUNT
            
            示例5：
            工单：你们服务太好了，想表扬一下客服小姐姐
            类别：OTHER
            
            现在请分类：
            工单：{ticket}
            类别：
            """;

    public TicketClassificationService(DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String classify(String ticketContent) {
        return chatClient.prompt()
                .user(u -> u.text(CLASSIFICATION_PROMPT).param("ticket", ticketContent))
                .call()
                .content()
                .trim();
    }
}