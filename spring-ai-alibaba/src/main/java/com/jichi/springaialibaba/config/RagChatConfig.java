package com.jichi.springaialibaba.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagChatConfig {

    @Bean
    public ChatClient ragChatClient(DashScopeChatModel chatModel, VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个企业知识库助手。
                        根据提供的参考资料回答用户问题。
                        如果参考资料中没有相关信息，明确告知用户"我在知识库中没有找到相关信息"，不要编造。
                        回答时可以引用来源文件名。
                        """)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(5)                   // 检索 5 条相关文档
                                        .similarityThreshold(0.6)  // 相似度低于 0.6 的不要
                                        .build())
                                .build()
                )
                .build();
    }
}