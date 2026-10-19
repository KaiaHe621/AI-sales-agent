package com.jichi.agent.service;

import com.jichi.agent.tools.CustomerServiceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceAgent {

    private final ChatClient chatClient;

    public CustomerServiceAgent(@Qualifier("dashScopeChatModel") ChatModel chatModel,
                                CustomerServiceTools tools) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个专业的电商客服助手，服务于一家耳机电商平台。
                        
                        你可以：
                        - 根据订单号查询物流状态（getOrderTracking）
                        - 搜索商品、查询库存和价格（searchProducts）
                        
                        工作原则：
                        - 需要数据时调用工具，绝对不要猜测或编造数据
                        - 如果用户提供的订单号格式不对（不是 ORD 开头），先提示用户确认
                        - 回答简洁友好，语气专业但不生硬
                        """)
                .defaultTools(tools)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}