package com.jichi.agent.service;

import com.jichi.agent.tools.AssistantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PersonalAssistantAgent {

    private final ChatClient chatClient;

    public PersonalAssistantAgent(@Qualifier("dashScopeChatModel") ChatModel chatModel,
                                   AssistantTools assistantTools) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个聪明的个人助理，名字叫小智。
                        
                        你可以：
                        - 查询任意城市的实时天气
                        - 告知当前日期、时间和星期
                        - 查询外汇汇率
                        - 帮用户创建提醒事项
                        
                        工作原则：
                        - 需要数据时主动调用工具，不要猜测或编造任何数据
                        - 回答简洁，重点突出，不要废话
                        - 用户一次问多个问题时，把所有相关工具都调完再统一回答
                        - 工具调用失败时，如实告知用户并说明原因
                        """)
                .defaultTools(assistantTools)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}