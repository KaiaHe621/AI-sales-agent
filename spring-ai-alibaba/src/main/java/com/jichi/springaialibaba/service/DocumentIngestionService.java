package com.jichi.springaialibaba.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DocumentIngestionService {

    // DashScope Embedding API 单批上限是 10 条，超过会报 400
    private static final int BATCH_SIZE = 10;

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.splitter = new TokenTextSplitter(512, 100, 5, 10000, true);
    }

    /**
     * 加载 PDF、切片、向量化、写入向量库，返回入库的 chunk 数量
     */
    public int ingestPdf(String filename, String category, Resource resource) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        List<Document> rawDocs = reader.get();

        rawDocs.forEach(doc -> {
            doc.getMetadata().put("source", filename);
            doc.getMetadata().put("category", category);
            doc.getMetadata().put("ingested_at", java.time.LocalDate.now().toString());
        });

        List<Document> chunks = splitter.apply(rawDocs);
        // 分批写入，每批最多 10 条，避免触发 DashScope 的 batch size 限制
        batchAdd(chunks);

        return chunks.size();
    }

    /**
     * 从文本内容创建文档并入库
     */
    public void ingestText(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        List<Document> chunks = splitter.apply(List.of(doc));
        batchAdd(chunks);
    }

    private void batchAdd(List<Document> docs) {
        for (int i = 0; i < docs.size(); i += BATCH_SIZE) {
            List<Document> batch = docs.subList(i, Math.min(i + BATCH_SIZE, docs.size()));
            vectorStore.add(batch);
        }
    }
}