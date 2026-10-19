package com.jichi.voiceshopping.agent;

import io.agentscope.core.ReActAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AgentMemoryPolicy {

    /**
     * 推荐 Agent 水位：1 轮 ≈ 2 条（user + assistant），4 轮 = 8 条
     */
    private static final int RECOMMEND_MEMORY_LIMIT = 8;

    /**
     * 情感 Agent 水位：情绪追踪需要更长上下文，40 条（约 20 轮）再裁
     */
    private static final int EMOTION_MEMORY_LIMIT = 40;

    /**
     * 意图 Agent：每轮清空，只靠 Prompt 注入的"最近 3 轮摘要"做上下文，
     * 防止老对话干扰当前意图判断（比如上一轮的 category 串到这一轮的 intent 里）
     */
    public void beforeIntentCall(ReActAgent agent) {
        if (agent.getMemory() != null) agent.getMemory().clear();
    }

    /**
     * 推荐 Agent：超过 4 轮就裁到 4 轮，避免 Token 线性增长 + 历史推荐污染新推荐
     */
    public void beforeRecommendCall(ReActAgent agent) {
        trimToLast(agent, RECOMMEND_MEMORY_LIMIT, "recommend");
    }

    /**
     * 情感 Agent：保留相对长的历史用于情绪连续性判断，超过 20 轮才裁
     */
    public void beforeEmotionCall(ReActAgent agent) {
        trimToLast(agent, EMOTION_MEMORY_LIMIT, "emotion");
    }

    /**
     * 用 deleteMessage(0) 循环从头裁，保留最后 limit 条
     */
    private void trimToLast(ReActAgent agent, int limit, String tag) {
        if (agent == null || agent.getMemory() == null) return;
        int size = agent.getMemory().getMessages().size();
        if (size <= limit) return;
        int removeCount = size - limit;
        for (int i = 0; i < removeCount; i++) {
            agent.getMemory().deleteMessage(0);
        }
        log.info("[MemoryPolicy] {} agent trimmed {} -> {}", tag, size, limit);
    }
}