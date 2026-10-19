package com.jichi.agent.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HallucinationGuard {

    private final ChatClient verifierClient;

    public HallucinationGuard(@Qualifier("dashScopeChatModel") ChatModel chatModel) {
        // 用轻量模型做验证，速度快成本低
        this.verifierClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个数据一致性检查员。
                        检查 Agent 的回答是否和工具实际返回的数据一致。
                        如果回答里出现了工具数据里没有的具体数字或事实，判定为不一致。
                        只输出 CONSISTENT 或 INCONSISTENT: [不一致的具体内容]
                        """)
                .build();
    }

    /**
     * 检查最终答案是否和工具数据一致
     *
     * @param toolResults  工具实际返回的数据（从日志或拦截层收集）
     * @param finalAnswer  Agent 生成的最终答案
     */
    public boolean isConsistent(String toolResults, String finalAnswer) {
        String result = verifierClient.prompt()
                .user("工具实际返回的数据：\n" + toolResults + "\n\nAgent 的回答：\n" + finalAnswer)
                .call()
                .content()
                .strip();

        return result.startsWith("CONSISTENT");
    }
}