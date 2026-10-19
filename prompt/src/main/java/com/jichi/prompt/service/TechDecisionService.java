package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechDecisionService {

    private final ChatClient chatClient;

    public TechDecisionService(DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个资深架构师，擅长技术方案评估。
                        评估时要展示完整的思考过程，不要直接跳结论。
                        """)
                .build();
    }

    public record TechEvaluation(
            List<String> pros,
            List<String> cons,
            List<String> risks,
            String recommendation,  // RECOMMEND / NOT_RECOMMEND / CONDITIONAL
            String reason
    ) {
    }

    public TechEvaluation evaluate(String techProposal, String context) {
        return chatClient.prompt()
                .user(String.format("""
                        请评估以下技术方案是否适合我们的场景。
                        
                        背景：%s
                        
                        方案：%s
                        
                        请先逐步分析优缺点和风险，再给出结构化结论。
                        """, context, techProposal))
                .call()
                .entity(TechEvaluation.class);
    }
}