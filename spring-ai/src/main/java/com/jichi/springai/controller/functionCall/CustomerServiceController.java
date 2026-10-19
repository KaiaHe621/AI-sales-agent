package com.jichi.springai.controller.functionCall;

import com.jichi.springai.tools.OrderQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    private final ChatClient chatClient;
    private final MessageWindowChatMemory chatMemory;

    public CustomerServiceController(
            ChatClient.Builder builder,
            OrderQueryTools orderQueryTools) {

        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        this.chatClient = builder
                .defaultSystem("""
                        你是一个电商平台的智能客服助手。
                        
                        你可以：
                        - 查询订单状态和物流
                        - 查询用户历史订单
                        - 搜索商品信息
                        
                        规则：
                        - 只回答与订单、商品相关的问题
                        - 需要查询时直接调用工具，不要编造数据
                        - 对用户友好耐心
                        """)
                .defaultTools(orderQueryTools)  // 全局注册工具，每次调用都带
                .build();
    }

    @PostMapping
    public String chat(@RequestBody CustomerServiceRequest request) {
        return chatClient.prompt()
                .user(request.message())
                // 按 userId 区分不同用户的会话记忆（1.1.x 写法）
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(request.userId().toString())
                        .build())
                .call()
                .content();
    }

    record CustomerServiceRequest(Long userId, String message) {
    }
}