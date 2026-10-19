package com.jichi.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MemoryExtractionService {

    private final ChatClient extractorClient;
    private final VectorStore vectorStore;

    public MemoryExtractionService(
            @Qualifier("dashScopeChatModel") ChatModel chatModel,
            VectorStore vectorStore) {
        this.extractorClient = ChatClient.builder(chatModel).build();
        this.vectorStore = vectorStore;
    }

    /**
     * 任务结束后调用：从对话记录中提炼有长期价值的信息
     *
     * @param userId      用户 ID（记忆按用户隔离）
     * @param taskSummary 任务对话的摘要或关键消息
     */
    public void extractAndStore(String userId, String taskSummary) {
        String extraction = extractorClient.prompt()
                .system("""
                        分析以下对话内容，提炼出值得长期记住的信息。
                        
                        值得记的（提取出来）：
                        - 用户明确告知的个人偏好、职业、技术栈、公司信息
                        - 用户的决策模式（关注什么因素）
                        - 重要的时间节点或计划
                        
                        不值得记的（忽略）：
                        - 临时性内容（打招呼、确认理解）
                        - 可以从公开信息查到的内容
                        - 模型自己的推理过程
                        
                        如果没有值得记的内容，直接输出：无
                        如果有，每条单独一行，格式：[类型] 内容
                        类型可选：偏好 / 背景 / 计划 / 经验
                        """)
                .user(taskSummary)
                .call()
                .content();

        if (extraction.isBlank() || extraction.strip().equals("无")) {
            return; // 没有值得记的内容，不写入
        }

        // 逐条存入向量库
        for (String line : extraction.split("\n")) {
            line = line.strip();
            if (line.isEmpty()) continue;
            vectorStore.add(List.of(new Document(
                    line,
                    Map.of("userId", userId,
                           "timestamp", System.currentTimeMillis(),
                           "source", "memory_extraction"))));
        }
    }
}