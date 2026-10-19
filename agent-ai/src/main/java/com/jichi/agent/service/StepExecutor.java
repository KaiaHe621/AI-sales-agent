package com.jichi.agent.service;

import com.jichi.agent.tools.AssistantTools;
import com.jichi.agent.tools.CustomerServiceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StepExecutor {

    private final ChatClient executorClient;

    public StepExecutor(@Qualifier("dashScopeChatModel") ChatModel chatModel,
                         AssistantTools assistantTools,
                         CustomerServiceTools customerServiceTools) {
        this.executorClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个步骤执行器。
                        你会收到一个具体的步骤描述，以及之前步骤的执行结果。
                        请使用合适的工具完成这个步骤，并返回执行结果。
                        如果这个步骤不需要工具，直接用你的知识完成并返回结果。
                        """)
                .defaultTools(assistantTools, customerServiceTools)
                .build();
    }

    /**
     * 执行单个步骤
     *
     * @param step            步骤描述
     * @param previousResults 之前各步骤的执行结果（key: 步骤编号, value: 结果）
     */
    public String execute(String step, Map<Integer, String> previousResults) {
        StringBuilder context = new StringBuilder();

        if (!previousResults.isEmpty()) {
            context.append("之前步骤的执行结果：\n");
            previousResults.forEach((stepNo, result) ->
                    context.append("Step ").append(stepNo).append(" 结果：").append(result).append("\n"));
            context.append("\n");
        }

        context.append("当前需要执行的步骤：").append(step);

        return executorClient.prompt()
                .user(context.toString())
                .call()
                .content();
    }
}