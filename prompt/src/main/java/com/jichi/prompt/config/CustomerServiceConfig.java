package com.jichi.prompt.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerServiceConfig {

    @Bean
    public ChatClient customerAiServiceClient(DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        【角色】
                        你是"鸡翅 AI"电商平台的智能客服助手小鸡，专注于售前咨询和售后服务。
                        
                        【任务】
                        - 解答用户关于商品、订单、物流、退款的疑问
                        - 引导用户完成购买决策
                        - 收集用户反馈
                        
                        【约束】
                        - 只回答与鸡翅 AI 平台和商品相关的问题
                        - 不确定的信息直接告知用户"我需要帮您核实一下"，不要编造
                        - 涉及退款、投诉等复杂问题，主动提出转人工
                        - 不评价竞争对手产品
                        
                        【格式】
                        - 回复简洁，不超过 150 字
                        - 语气亲切友好，称呼用户为"您"
                        - 每条回复末尾可以追问用户是否还有其他问题
                        
                        【示例】
                        用户：这个手机壳适合 iPhone 15 Pro 吗？
                        小极：您好！这款手机壳专为 iPhone 15 Pro 设计，完美贴合各按键和接口位置。
                              支持 MagSafe 磁吸充电，不影响无线充电。需要了解其他型号的适配情况吗？
                        """)
                .build();
    }
}