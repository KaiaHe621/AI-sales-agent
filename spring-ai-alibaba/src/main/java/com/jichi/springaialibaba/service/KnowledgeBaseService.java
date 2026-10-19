package com.jichi.springaialibaba.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private static final int BATCH_SIZE = 10;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final TokenTextSplitter splitter;

    public KnowledgeBaseService(VectorStore vectorStore, DashScopeChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.splitter = new TokenTextSplitter(512, 100, 5, 10000, true);

        // RAG + 多轮对话记忆，两个 Advisor 叠加
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个企业知识库助手，基于提供的参考资料回答问题。
                        规则：
                        1. 只根据参考资料中的信息回答，不要编造
                        2. 如果资料中没有相关信息，直接说"我在知识库中没有找到相关信息"
                        3. 回答时可以引用来源（如：根据《xxx文档》）
                        """)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(5).similarityThreshold(0.6).build())
                                .build(),
                        MessageChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                        .build()
                        ).build()
                )
                .build();
    }

    public record IngestResult(String filename, String category, int chunks) {
    }

    public record SearchResult(String content, String source, String category) {
    }

    /**
     * 上传并入库文档，分批写入避免 DashScope batch size 限制
     */
    public IngestResult ingestDocument(String filename, String category, Resource resource) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        List<Document> rawDocs = reader.get();

        rawDocs.forEach(doc -> {
            doc.getMetadata().put("source", filename);
            doc.getMetadata().put("category", category);
        });

        List<Document> chunks = splitter.apply(rawDocs);
        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            vectorStore.add(chunks.subList(i, Math.min(i + BATCH_SIZE, chunks.size())));
        }

        return new IngestResult(filename, category, chunks.size());
    }

    /**
     * 带会话 ID 的问答，支持多轮对话
     */
    public String ask(String question, String conversationId) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId))
                .call()
                .content();
    }

    /**
     * 不带历史的单次问答
     */
    public String askOnce(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * 查看检索结果（调试用）
     */
    public List<SearchResult> search(String query, int topK) {
        return vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).topK(topK).build()
                ).stream()
                .map(doc -> new SearchResult(
                        doc.getText(),
                        (String) doc.getMetadata().getOrDefault("source", "unknown"),
                        (String) doc.getMetadata().getOrDefault("category", "unknown")
                ))
                .toList();
    }
}