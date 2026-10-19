package com.jichi.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TaskPlanner {

    private final ChatClient plannerClient;

    public TaskPlanner(@Qualifier("dashScopeChatModel") ChatModel chatModel) {
        this.plannerClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个任务规划专家。
                        
                        收到用户任务后，把它分解为具体的执行步骤。
                        
                        输出格式（严格遵守）：
                        STEP 1: [步骤描述]
                        STEP 2: [步骤描述]
                        STEP 3: [步骤描述]
                        ...
                        
                        要求：
                        - 每步描述要具体，说明做什么、用什么工具、期望结果
                        - 步骤数量合理，不要过于细碎（3-8 步为宜）
                        - 标注步骤依赖（如果某步依赖上一步的结果，说明"依赖 Step X"）
                        - 只输出步骤，不要解释
                        """)
                .build();
    }

    public List<String> plan(String task, List<String> availableTools) {
        String toolsDescription = String.join("、", availableTools);

        String planText = plannerClient.prompt()
                .user("可用工具：" + toolsDescription + "\n\n任务：" + task)
                .call()
                .content();

        return parsePlan(planText);
    }

    private List<String> parsePlan(String planText) {
        return Arrays.stream(planText.split("\n"))
                .filter(line -> line.matches("STEP \\d+:.*"))
                .map(line -> line.replaceFirst("STEP \\d+:\\s*", "").strip())
                .toList();
    }
}